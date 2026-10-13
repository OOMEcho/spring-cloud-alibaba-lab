package cn.oomecho.cloud.order.service;

import cn.oomecho.cloud.order.domain.entity.OrderOutbox;
import cn.oomecho.cloud.order.integration.OrderEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 发件箱批量投递编排；网络调用不占用订单事务，确认后丢失本地更新会安全重复投递
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
@ConditionalOnProperty(name = "lab.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxLocalService local;
    private final OrderEventSender sender;

    /**
     * @param local 持久化短事务 @param sender 单次投递
     */
    public OutboxPublisher(OutboxLocalService local, OrderEventSender sender) {
        this.local = local;
        this.sender = sender;
    }

    /**
     * 扫描一个有限批次；失败保留原 payload 和 ID，后续周期或人工入口继续。
     */
    public void publishBatch() {
        for (var id : local.due()) {
            OrderOutbox event;
            try {
                event = local.claim(id);
            } catch (RuntimeException claimFailure) {
                // 某条全局事件仍被锁定或领取失败，不阻塞同批其他已提交事件。
                // 领取事务会回滚，事件保留原状态，由下一周期重新检查。
                log.warn("事件暂不能领取，跳过本条，eventId={}，errorType={}", id, claimFailure.getClass().getSimpleName());
                continue;
            }
            if (event == null) continue;
            String previous = MDC.get("requestId");
            try {
                if (event.getRequestId() != null) MDC.put("requestId", event.getRequestId());
                sender.send(event);
                local.sent(event);
                log.info("事件 broker 接收确认，eventId={}，orderNo={}", id, event.getOrderNo());
            } catch (Exception e) {
                // 不把连接地址、账号或底层异常写入可公开查询的 lastError。
                try {
                    local.failed(event, "消息接收结果未确认，将按策略重试");
                } catch (RuntimeException updateFailure) {
                    // 状态回写失败时保留领取租约，过期后可再次领取；继续处理同批其他事件。
                    log.warn("事件重试状态暂未保存，eventId={}，errorType={}", id, updateFailure.getClass().getSimpleName());
                }
                log.warn("事件发布待重试，eventId={}，attempt={}，errorType={}", id, event.getAttempts(), e.getClass().getSimpleName());
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } finally {
                if (previous == null) MDC.remove("requestId");
                else MDC.put("requestId", previous);
            }
        }
    }
}
