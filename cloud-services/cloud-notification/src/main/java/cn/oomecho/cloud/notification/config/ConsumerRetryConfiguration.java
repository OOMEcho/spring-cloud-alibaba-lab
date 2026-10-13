package cn.oomecho.cloud.notification.config;

import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 重试耗尽后明确 reject 且不立即 requeue，由 broker 将消息转入死信队列
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Configuration(proxyBeanMethods = false)
public class ConsumerRetryConfiguration {
    /**
     * @return Spring Boot 监听容器重试器使用的恢复策略
     */
    @Bean
    public MessageRecoverer orderEventRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }
}
