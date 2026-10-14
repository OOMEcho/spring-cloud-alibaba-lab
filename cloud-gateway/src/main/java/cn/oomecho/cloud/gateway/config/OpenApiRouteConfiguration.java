package cn.oomecho.cloud.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合五个服务的 OpenAPI 文档；仅展示公开业务路径，在线调试统一经过网关。
 * 文档路由独立于 Nacos 动态业务路由，不扩大业务路由的管理接口访问范围。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiRouteConfiguration {
    /** 固定文档来源及其允许展示的业务路径前缀。 */
    private static final List<DocumentSource> SOURCES = List.of(
            new DocumentSource("user", "/users"),
            new DocumentSource("order", "/orders"),
            new DocumentSource("product", "/products"),
            new DocumentSource("stock", "/stocks"),
            new DocumentSource("notification", "/notifications"));

    /**
     * 将固定 GET 文档地址转发到对应服务，使用服务发现而非固定服务端口。
     *
     * @param builder 网关路由构建器
     * @param mapper 应用 JSON 映射器
     * @return 与动态业务路由共同生效的文档路由
     */
    @Bean
    public RouteLocator openApiRoutes(RouteLocatorBuilder builder, ObjectMapper mapper) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        for (DocumentSource source : SOURCES) {
            routes.route("openapi-" + source.service(), route -> route
                    .path("/docs/" + source.service()).and().method(HttpMethod.GET)
                    .filters(filters -> filters.setPath("/v3/api-docs")
                            .modifyResponseBody(String.class, String.class, MediaType.APPLICATION_JSON_VALUE,
                                    (exchange, body) -> {
                                        // 上游错误保留原状态与响应，避免把故障包装成成功文档。
                                        if (body == null) {
                                            return Mono.empty();
                                        }
                                        if (exchange.getResponse().getStatusCode() != null
                                                && !exchange.getResponse().getStatusCode().is2xxSuccessful()) {
                                            return Mono.just(body);
                                        }
                                        return Mono.fromCallable(() -> publicDocument(body, source, mapper));
                                    }))
                    .uri("lb://cloud-" + source.service()));
        }
        return routes.build();
    }

    /**
     * 保留当前服务公开业务接口，并替换调试服务器地址；不修改服务自身的文档。
     *
     * @param body 上游 OpenAPI JSON
     * @param source 当前文档来源
     * @param mapper JSON 映射器
     * @return 网关入口使用的 OpenAPI JSON
     */
    private String publicDocument(String body, DocumentSource source, ObjectMapper mapper) {
        try {
            if (!(mapper.readTree(body) instanceof ObjectNode document)
                    || !document.hasNonNull("openapi")
                    || !(document.get("paths") instanceof ObjectNode paths)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "上游未返回有效 OpenAPI 文档");
            }
            List<String> excluded = new ArrayList<>();
            paths.fieldNames().forEachRemaining(path -> {
                if (!path.equals(source.pathPrefix()) && !path.startsWith(source.pathPrefix() + "/")) {
                    excluded.add(path);
                } else if (paths.get(path) instanceof ObjectNode pathItem) {
                    // 下级 servers 可以覆盖根级配置，同样移除以保证调试经过网关。
                    pathItem.remove("servers");
                    pathItem.elements().forEachRemaining(operation -> {
                        if (operation instanceof ObjectNode object) {
                            object.remove("servers");
                        }
                    });
                }
            });
            paths.remove(excluded);
            document.putArray("servers").addObject().put("url", "/api")
                    .put("description", "通过网关调用公开业务接口");
            return mapper.writeValueAsString(document);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "上游 OpenAPI 文档解析失败", exception);
        }
    }

    /**
     * 固定文档来源，不接受请求参数或远程配置指定任意上游。
     *
     * @param service 服务名称后缀
     * @param pathPrefix 允许展示的业务路径前缀
     */
    private record DocumentSource(String service, String pathPrefix) {
    }
}
