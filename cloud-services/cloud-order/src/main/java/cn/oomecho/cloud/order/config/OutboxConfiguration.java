package cn.oomecho.cloud.order.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 发件箱本地持久化配置；即使暂时关闭 RabbitMQ 投递仍保存订单事件
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfiguration {
    /**
     * @return 发件箱时间统一使用 UTC，可在测试中替换
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock outboxClock() {
        return Clock.systemUTC();
    }
}
