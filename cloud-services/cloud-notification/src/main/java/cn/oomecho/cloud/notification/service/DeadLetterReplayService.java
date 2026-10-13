package cn.oomecho.cloud.notification.service;

import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.common.rabbit.config.RabbitMessagingProperties;
import cn.oomecho.cloud.notification.domain.vo.DeadLetterReplayVO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 人工修复后的单条死信重放；新发布确认之前不 ACK 原消息，故障可能重复但不会主动丢弃
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
@ConditionalOnProperty(name = "lab.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterReplayService {
    private final RabbitTemplate template;
    private final RabbitMessagingProperties properties;

    /**
     * @param template correlated confirms + returns + mandatory @param properties 当前项目独立队列名称
     */
    public DeadLetterReplayService(RabbitTemplate template, RabbitMessagingProperties properties) {
        this.template = template;
        this.properties = properties;
    }

    /**
     * @return 原事件重新发布结果；空队列不算错误，消费失败仍可能重新进入死信
     */
    public DeadLetterReplayVO replayOne() {
        try {
            return template.execute(channel -> {
                var delivery = channel.basicGet(properties.deadQueue(), false);
                if (delivery == null) return new DeadLetterReplayVO(false, null);
                long tag = delivery.getEnvelope().getDeliveryTag();
                try {
                    var headers = new DefaultMessagePropertiesConverter().toMessageProperties(delivery.getProps(), delivery.getEnvelope(), "UTF-8");
                    headers.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    var message = new Message(delivery.getBody(), headers);
                    var correlation = new CorrelationData("redrive:" + UUID.randomUUID());
                    template.send(properties.exchange(), properties.routingKey(), message, correlation);
                    var confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                    if (!confirm.isAck() || correlation.getReturned() != null)
                        throw new IllegalStateException("死信重发未获接收确认");
                    channel.basicAck(tag, false);
                    return new DeadLetterReplayVO(true, headers.getMessageId());
                } catch (Exception failure) {
                    if (channel.isOpen()) channel.basicNack(tag, false, true);
                    if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
                    throw failure;
                }
            });
        } catch (Exception failure) {
            throw new BusinessException(503, 50305, "死信重放结果未确认，请查询通知并检查死信队列");
        }
    }
}
