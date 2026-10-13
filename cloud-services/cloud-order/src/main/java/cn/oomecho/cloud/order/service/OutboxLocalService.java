package cn.oomecho.cloud.order.service;

import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.config.OutboxProperties;
import cn.oomecho.cloud.order.domain.entity.OrderOutbox;
import cn.oomecho.cloud.order.mapper.OrderOutboxMapper;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 发件箱短事务；领取提交后才访问 broker，租约过期与人工重试均沿用原事件 ID
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
public class OutboxLocalService {
    private final OrderOutboxMapper mapper;
    private final OutboxProperties properties;
    private final Clock clock;
    private final boolean seataEnabled;

    /**
     * @param mapper 发件箱数据访问 @param properties 重试参数 @param clock UTC 时间 @param seataEnabled 全局客户端是否启用
     */
    public OutboxLocalService(OrderOutboxMapper mapper, OutboxProperties properties, Clock clock, @Value("${seata.enabled:false}") boolean seataEnabled) {
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
        this.seataEnabled = seataEnabled;
    }

    /**
     * @param orderNo 原订单号 @return 事件列表，未确认的订单为空
     */
    public List<OrderOutbox> byOrder(String orderNo) {
        return mapper.byOrder(orderNo);
    }

    /**
     * @return 当前可尝试的有限事件列表，领取仍需单独条件更新
     */
    public List<String> due() {
        return mapper.due(LocalDateTime.now(clock), properties.batchSize(), seataEnabled);
    }

    /**
     * @param id 事件 ID @return 当前发布者领取的行，已被其他实例领取时返回 null
     */
    @GlobalLock(lockRetryTimes = 1, lockRetryInterval = 10)
    @Transactional
    public OrderOutbox claim(String id) {
        var now = LocalDateTime.now(clock);
        var token = UUID.randomUUID().toString();
        // @GlobalLock 在本地提交前检查 TC 锁，禁止全局事务未完成时向 RabbitMQ 发送。
        // 清理和领取都先按同一主键锁当前事件，避免范围清理与主键领取交叉导致死锁。
        mapper.expireExhausted(id, now, properties.maxAttempts());
        if (mapper.claim(id, now, token, now.plusSeconds(properties.leaseSeconds()), properties.maxAttempts()) == 0)
            return null;
        return mapper.selectById(id);
    }

    /**
     * @param event 已领取行，包含令牌
     */
    @Transactional
    public void sent(OrderOutbox event) {
        mapper.sent(event.getEventId(), event.getLeaseToken(), LocalDateTime.now(clock));
    }

    /**
     * @param event 已领取行 @param error 不含基础设施地址的公开原因
     */
    @Transactional
    public void failed(OrderOutbox event, String error) {
        long delay = Math.min(60, properties.retryBaseSeconds() * (1L << Math.min(6, event.getAttempts() - 1)));
        String state = event.getAttempts() >= properties.maxAttempts() ? "FAILED" : "PENDING";
        mapper.failed(event.getEventId(), event.getLeaseToken(), state, LocalDateTime.now(clock).plusSeconds(delay), error);
    }

    /**
     * @param id 事件 ID @return 持久化状态，SENT 不等于已消费
     */
    public OrderOutbox require(String id) {
        var row = mapper.selectById(id);
        if (row == null) throw new BusinessException(404, 40405, "订单事件不存在");
        return row;
    }

    /**
     * @param id 原事件 ID @return 已安排重新投递的事件
     */
    @GlobalLock(lockRetryTimes = 1, lockRetryInterval = 10)
    @Transactional
    public OrderOutbox retry(String id) {
        if (require(id).getGlobalXid() != null && !seataEnabled)
            throw new BusinessException(503, 50309, "全局事件必须启用 Seata 全局锁检查后才能重试");
        if (mapper.retry(id, LocalDateTime.now(clock)) == 0)
            throw new BusinessException(409, 40907, "已接收或正在投递的事件不能重新安排");
        return require(id);
    }
}
