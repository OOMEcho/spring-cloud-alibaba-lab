package cn.oomecho.cloud.order.integration;

import cn.oomecho.cloud.order.domain.entity.OrderOutbox;

/**
 * 单次事件投递适配边界；成功表示 broker 接收且没有路由退回
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
public interface OrderEventSender {
    /**
     * @param event 已领取的不可变发件箱行 @throws Exception 无法确认接收时由调用者安排重试
     */
    void send(OrderOutbox event) throws Exception;
}
