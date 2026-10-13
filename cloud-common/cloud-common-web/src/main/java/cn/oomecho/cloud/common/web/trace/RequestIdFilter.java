package cn.oomecho.cloud.common.web.trace;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求 ID 过滤器；关联订单服务与用户服务的日志
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public class RequestIdFilter extends OncePerRequestFilter {
    /**
     * 请求与响应使用同一个 HTTP 请求 ID 头。
     */
    public static final String HEADER = "X-Request-Id";
    /**
     * 业务日志使用的 MDC 键，Feign 拦截器从该键读取。
     */
    public static final String MDC_KEY = "requestId";

    /**
     * 当前服务名；用于学习环境观察请求实际落到哪个实例。
     */
    private final String serviceName;

    /**
     * @param serviceName 当前服务名
     */
    public RequestIdFilter(@Value("${spring.application.name:unknown}") String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 复用格式合法的请求 ID，或生成新 ID；请求结束后清理线程上下文。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param chain    后续过滤链
     * @throws ServletException 过滤链处理失败
     * @throws IOException      响应写入失败
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(HEADER);
        // 限制长度与字符，避免任意客户端输入进入日志。
        if (requestId == null || !requestId.matches("[A-Za-z0-9-]{1,64}")) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        response.setHeader("X-Service-Instance", serviceName + ":" + request.getLocalPort());
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
