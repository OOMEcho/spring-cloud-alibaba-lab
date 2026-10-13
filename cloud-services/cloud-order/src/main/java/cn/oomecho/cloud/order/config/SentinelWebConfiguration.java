package cn.oomecho.cloud.order.config;

import cn.oomecho.cloud.common.core.result.Result;
import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentinel Web 拦截响应配置；直接配置 URL 规则时也使用统一 JSON 格式。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Configuration(proxyBeanMethods = false)
public class SentinelWebConfiguration {
    /**
     * 避免框架默认文本响应，保留限流与熔断的 HTTP 错误语义。
     *
     * @param mapper 应用 JSON 序列化器
     * @return Web 资源阻断处理器
     */
    @Bean
    public BlockExceptionHandler blockExceptionHandler(ObjectMapper mapper) {
        return (request, response, resource, exception) -> {
            boolean degraded = exception instanceof DegradeException;
            response.setStatus(degraded ? 503 : 429);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getOutputStream(), Result.failure(degraded ? 50302 : 42901,
                    degraded ? "服务已熔断，请稍后重试" : "请求过于频繁，请稍后重试"));
        };
    }
}
