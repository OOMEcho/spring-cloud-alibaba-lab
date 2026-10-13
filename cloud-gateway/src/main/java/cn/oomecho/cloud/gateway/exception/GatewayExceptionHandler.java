package cn.oomecho.cloud.gateway.exception;

import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.gateway.filter.RequestIdWebFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 网关异常处理；上游连接故障返回不可用，未知异常保留内部错误语义。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class GatewayExceptionHandler implements WebExceptionHandler, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    private final ObjectMapper mapper;

    /**
     * @param mapper 应用 JSON 序列化器
     */
    public GatewayExceptionHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 生成统一错误响应，不覆盖上游已经返回的正常业务错误响应。
     *
     * @param exchange  请求上下文
     * @param exception 路由或网络异常
     * @return 写入响应的异步结果
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (exception instanceof ResponseStatusException responseStatus) {
            status = responseStatus.getStatusCode();
        } else if (isNetworkFailure(exception)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        String message = switch (status.value()) {
            case 404 -> "网关路由不存在";
            case 503, 504 -> "上游服务暂不可用，请稍后重试";
            default -> "网关请求处理失败";
        };
        log.warn("网关请求失败，requestId={}，path={}，status={}，type={}",
                exchange.getAttribute(RequestIdWebFilter.HEADER),
                exchange.getRequest().getPath().value(), status.value(), exception.getClass().getSimpleName());
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = mapper.writeValueAsBytes(Result.failure(status.value() * 100, message));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        } catch (IOException serializationFailure) {
            return Mono.error(serializationFailure);
        }
    }

    /**
     * 只把连接、读取等网络异常识别为上游不可用，不隐藏代码错误。
     *
     * @param exception 待检查异常
     * @return 是否为网络异常
     */
    private boolean isNetworkFailure(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof IOException || current instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 先于默认错误页面处理器运行的优先级
     */
    @Override
    public int getOrder() {
        return -2;
    }
}
