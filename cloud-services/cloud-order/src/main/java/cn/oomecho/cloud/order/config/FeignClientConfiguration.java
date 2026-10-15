package cn.oomecho.cloud.order.config;


import cn.oomecho.cloud.common.web.trace.RequestIdFilter;
import com.alibaba.csp.sentinel.Tracer;
import feign.FeignException;
import feign.RequestInterceptor;
import feign.Retryer;
import org.apache.seata.core.context.RootContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * 所有订单下游 Feign 请求的共享配置；传递请求 ID 并关闭自动重试
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Configuration(proxyBeanMethods = false)
public class FeignClientConfiguration {
    /**
     * Sentinel Feign 默认会统计 HTTP 异常；排除明确的 4xx 业务拒绝。
     * 429 表示依赖容量受限，仍计入故障；连接失败、超时、5xx 和解码异常也保留统计。
     * 此谓词作用于订单进程的 Sentinel 异常统计，不改变实际响应或回退策略。
     */
    @PostConstruct
    public void configureSentinelExceptionStatistics() {
        Tracer.setExceptionPredicate(exception -> !(exception instanceof FeignException feign)
                || feign.status() < 400 || feign.status() >= 500 || feign.status() == 429);
    }

    /**
     * 同步调用不自动重试，避免后续写接口在失败时被重复执行。
     *
     * @return 不重试策略
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 向用户服务传递请求 ID，便于从两端日志还原一次请求。
     *
     * @return Feign 请求拦截器
     */
    @Bean
    public RequestInterceptor requestIdInterceptor() {
        return template -> {
            String xid = RootContext.getXID();
            if (xid != null) template.header(RootContext.KEY_XID, xid);
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                template.header(RequestIdFilter.HEADER, requestId);
            }
        };
    }
}
