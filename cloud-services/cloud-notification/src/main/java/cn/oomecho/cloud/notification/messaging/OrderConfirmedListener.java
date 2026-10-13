package cn.oomecho.cloud.notification.messaging;

import cn.oomecho.cloud.notification.service.NotificationService;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 订单事件消费入口；独立事务服务提交返回后容器才 ACK，异常由有限重试和死信处理
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Component
@ConditionalOnProperty(name = "lab.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OrderConfirmedListener {
    private final NotificationService service;

    /**
     * @param service inbox 与业务记录本地事务
     */
    public OrderConfirmedListener(NotificationService service) {
        this.service = service;
    }

    /**
     * @param message broker 投递；不捕获业务异常后假称成功
     */
    @RabbitListener(queues = "${lab.messaging.queue}")
    public void receive(Message message) {
        Object id = message.getMessageProperties().getHeaders().get("X-Request-Id");
        String previous = MDC.get("requestId");
        try {
            if (id != null) {
                String value = id.toString().replaceAll("[^a-zA-Z0-9_-]", "");
                MDC.put("requestId", value.substring(0, Math.min(64, value.length())));
            }
            service.consume(message);
        } finally {
            if (previous == null) MDC.remove("requestId");
            else MDC.put("requestId", previous);
        }
    }
}
