package cn.oomecho.cloud.common.rabbit.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 本项目订单事件 RabbitMQ 拓扑名称；生产和消费端必须使用同一组配置
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Validated
@ConfigurationProperties("lab.messaging")
public record RabbitMessagingProperties(
        @NotBlank String exchange, @NotBlank String queue,
        @NotBlank String deadExchange, @NotBlank String deadQueue,
        @NotBlank String routingKey, @NotBlank String deadRoutingKey) {
}
