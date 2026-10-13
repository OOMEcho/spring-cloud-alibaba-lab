package cn.oomecho.cloud.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关请求 ID 过滤器；通过请求头和 exchange 传递，避免响应式线程切换导致 MDC 串值。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class RequestIdWebFilter implements WebFilter, Ordered {
    /**
     * 与业务服务一致的请求 ID 头名称。
     */
    public static final String HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdWebFilter.class);

    /**
     * 在路由前设置请求 ID，业务服务复用该 ID，返回时记录请求结果。
     *
     * @param exchange 当前请求上下文
     * @param chain    后续过滤链
     * @return 异步过滤结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String supplied = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = supplied != null && supplied.matches("[A-Za-z0-9-]{1,64}")
                ? supplied : UUID.randomUUID().toString();
        // exchange 属性在装饰后的请求和异常处理器间共享，错误日志也能取得请求 ID。
        exchange.getAttributes().put(HEADER, requestId);
        ServerWebExchange traced = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(HEADER, requestId))).build();
        traced.getResponse().getHeaders().set(HEADER, requestId);
        return chain.filter(traced).doFinally(signal -> log.info("网关请求结束，requestId={}，path={}，status={}",
                requestId, traced.getRequest().getPath().value(), traced.getResponse().getStatusCode()));
    }

    /**
     * @return 在网关路由过滤器前运行的优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
