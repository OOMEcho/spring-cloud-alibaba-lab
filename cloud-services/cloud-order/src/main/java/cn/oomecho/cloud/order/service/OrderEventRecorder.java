package cn.oomecho.cloud.order.service;

import cn.oomecho.cloud.api.order.event.OrderConfirmedEvent;
import cn.oomecho.cloud.order.domain.entity.Order;
import cn.oomecho.cloud.order.domain.entity.OrderOutbox;
import cn.oomecho.cloud.order.mapper.OrderOutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 在确认订单的原事务中写入不可变事件；序列化或插入失败必须回滚确认状态
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
public class OrderEventRecorder {
    private final OrderOutboxMapper mapper;
    private final ObjectMapper json;
    private final Clock clock;

    /**
     * @param mapper 本服务发件箱 @param json JSON 序列化器 @param clock UTC 时钟
     */
    public OrderEventRecorder(OrderOutboxMapper mapper, ObjectMapper json, Clock clock) {
        this.mapper = mapper;
        this.json = json;
        this.clock = clock;
    }

    /**
     * @param order 已在当前事务中确认的订单
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Order order) {
        var eventId = UUID.randomUUID().toString();
        var event = new OrderConfirmedEvent(eventId, "ORDER_CONFIRMED", 1, clock.instant().toString(),
                order.getOrderNo(), order.getUserId(), order.getProductId(), order.getProductName(),
                order.getQuantity(), order.getUnitPrice(), order.getTotalAmount());
        var row = new OrderOutbox();
        row.setEventId(eventId);
        row.setOrderNo(order.getOrderNo());
        row.setGlobalXid(order.getGlobalXid());
        row.setEventType(event.eventType());
        row.setStatus("PENDING");
        row.setAttempts(0);
        row.setCreatedAt(LocalDateTime.now(clock));
        row.setNextAttemptAt(row.getCreatedAt());
        String requestId = MDC.get("requestId");
        row.setRequestId(requestId == null ? null : requestId.substring(0, Math.min(64, requestId.length())));
        try {
            row.setPayload(json.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("订单事件序列化失败", e);
        }
        // 订单号+事件类型唯一键再次保护幂等；事务外的 RabbitMQ 连接不参与该写入。
        mapper.insert(row);
    }
}
