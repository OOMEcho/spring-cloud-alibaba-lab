package cn.oomecho.cloud.order.integration;

import cn.oomecho.cloud.common.rabbit.config.RabbitMessagingProperties;
import cn.oomecho.cloud.order.config.OutboxProperties;
import cn.oomecho.cloud.order.domain.entity.OrderOutbox;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 单次发布；确认和路由退回同时检查，不在这里偷偷重试或生成新事件
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Component
@ConditionalOnProperty(name = "lab.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitOrderEventSender implements OrderEventSender {
    private final RabbitTemplate template;
    private final RabbitMessagingProperties messaging;
    private final OutboxProperties properties;

    /**
     * @param template 开启 confirms、returns 和 mandatory 的模板 @param messaging 拓扑 @param properties 确认超时
     */
    public RabbitOrderEventSender(RabbitTemplate template, RabbitMessagingProperties messaging, OutboxProperties properties) {
        this.template = template;
        this.messaging = messaging;
        this.properties = properties;
    }

    /**
     * @param event 不可变事件；每次相关确认 ID 唯一，messageId 始终等于业务 eventId
     */
    @Override
    public void send(OrderOutbox event) throws Exception {
        var headers = new MessageProperties();
        headers.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        headers.setContentEncoding("UTF-8");
        headers.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        headers.setMessageId(event.getEventId());
        headers.setType(event.getEventType());
        if (event.getRequestId() != null) headers.setHeader("X-Request-Id", event.getRequestId());
        var correlation = new CorrelationData(event.getEventId() + ":" + UUID.randomUUID());
        template.send(messaging.exchange(), messaging.routingKey(),
                new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), headers), correlation);
        var confirm = correlation.getFuture().get(properties.confirmTimeoutSeconds(), TimeUnit.SECONDS);
        // 无路由的消息也可能收到 ACK；必须检查 mandatory return，不能误标为 SENT。
        if (!confirm.isAck() || correlation.getReturned() != null)
            throw new IllegalStateException("消息未被有效队列接收");
    }
}
