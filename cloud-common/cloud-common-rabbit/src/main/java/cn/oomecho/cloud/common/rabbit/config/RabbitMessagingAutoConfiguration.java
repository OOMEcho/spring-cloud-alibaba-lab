package cn.oomecho.cloud.common.rabbit.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 订单消息拓扑自动配置；仅被订单、通知服务按需引用，使用持久化 quorum 队列
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@AutoConfiguration
@ConditionalOnProperty(name = "lab.messaging.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitMessagingAutoConfiguration {
    /**
     * 主队列有限积压并启用至少一次死信转移；死信队列不自动消费以便人工排查。
     *
     * @param p 两端一致的拓扑名称
     * @return exchanges、queues 与 bindings，RabbitAdmin 幂等声明
     */
    @Bean
    public Declarables orderEventTopology(RabbitMessagingProperties p) {
        var main = QueueBuilder.durable(p.queue()).quorum()
                .withArgument("x-dead-letter-exchange", p.deadExchange())
                .withArgument("x-dead-letter-routing-key", p.deadRoutingKey())
                .withArgument("x-dead-letter-strategy", "at-least-once")
                .withArgument("x-overflow", "reject-publish")
                .withArgument("x-max-length", 10000).build();
        var dead = QueueBuilder.durable(p.deadQueue()).quorum().build();
        var exchange = new DirectExchange(p.exchange(), true, false);
        var deadExchange = new DirectExchange(p.deadExchange(), true, false);
        return new Declarables(exchange, deadExchange, main, dead,
                BindingBuilder.bind(main).to(exchange).with(p.routingKey()),
                BindingBuilder.bind(dead).to(deadExchange).with(p.deadRoutingKey()));
    }
}
