package cn.oomecho.cloud.notification.service;

import cn.oomecho.cloud.api.order.event.OrderConfirmedEvent;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.notification.domain.vo.OrderNotificationVO;
import cn.oomecho.cloud.notification.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 幂等创建站内通知；校验契约后在同一事务完成 inbox 与业务记录，失败全部回滚
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationMapper mapper;
    private final ObjectMapper json;
    private final Validator validator;

    /**
     * @param mapper 本服务 Mapper @param json 只反序列化固定事件类型 @param validator 契约校验
     */
    public NotificationService(NotificationMapper mapper, ObjectMapper json, Validator validator) {
        this.mapper = mapper;
        this.json = json;
        this.validator = validator;
    }

    /**
     * @param message 原始消息；messageId 与 JSON 中 eventId 必须一致
     * @return 首次处理 true；完全相同事件的重复投递 false
     */
    @Transactional
    public boolean consume(Message message) {
        OrderConfirmedEvent event;
        String hash;
        try {
            if (message.getBody().length > 65536) throw new IllegalArgumentException("事件超过大小限制");
            event = json.readValue(message.getBody(), OrderConfirmedEvent.class);
            if (event == null || !validator.validate(event).isEmpty())
                throw new IllegalArgumentException("事件不符合 V1 契约");
            if (!UUID.fromString(event.eventId()).toString().equals(event.eventId()))
                throw new IllegalArgumentException("事件 ID 格式错误");
            Instant.parse(event.occurredAt());
            if (!event.eventId().equals(message.getMessageProperties().getMessageId()))
                throw new IllegalArgumentException("消息 ID 与事件 ID 不一致");
            if (event.totalAmount().compareTo(event.unitPrice().multiply(java.math.BigDecimal.valueOf(event.quantity()))) != 0)
                throw new IllegalArgumentException("事件金额与快照不一致");
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(message.getBody()));
        } catch (Exception invalid) {
            throw new IllegalArgumentException("订单事件格式或快照校验失败", invalid);
        }
        log.info("开始处理订单确认事件，eventId={}，orderNo={}", event.eventId(), event.orderNo());
        var now = LocalDateTime.now(Clock.systemUTC());
        if (mapper.claim(event.eventId(), hash, now) == 0) {
            // 已处理的 ID 不能被另一份内容复用；重复消息只能返回原业务结果。
            if (!hash.equals(mapper.hash(event.eventId())))
                throw new IllegalArgumentException("相同事件 ID 的内容发生变化");
            log.info("订单事件重复投递已去重，eventId={}", event.eventId());
            return false;
        }
        mapper.insert(event.eventId(), event.orderNo(), event.userId(),
                "订单 " + event.orderNo() + " 已确认，商品：" + event.productName() + "，金额：" + event.totalAmount() + " 元", event.totalAmount(), now);
        log.info("站内订单通知已创建，eventId={}，orderNo={}", event.eventId(), event.orderNo());
        return true;
    }

    /**
     * @param orderNo 原订单号 @return 已保存的站内通知，未消费完成返回 40408
     */
    public OrderNotificationVO detail(String orderNo) {
        var row = mapper.find(orderNo);
        if (row == null) throw new BusinessException(404, 40408, "订单通知尚未创建");
        return row;
    }
}
