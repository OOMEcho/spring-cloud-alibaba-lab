package cn.oomecho.cloud.order.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单确认事件发件箱；状态、租约与确认结果均持久化
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Data
@TableName("lab_order_outbox")
public class OrderOutbox {
    /**
     * 稳定事件 ID，所有重投保持不变。
     */
    @TableId(type = IdType.INPUT)
    private String eventId;
    /**
     * 原订单号。
     */
    private String orderNo;
    /**
     * 固定 ORDER_CONFIRMED。
     */
    private String eventType;
    /**
     * 不可变 JSON 快照。
     */
    private String payload;
    /**
     * 原 HTTP 请求 ID。
     */
    private String requestId;
    /**
     * PENDING、PUBLISHING、SENT、FAILED；SENT 仅表示 broker 已接收。
     */
    private String status;
    /**
     * 已领取的发布次数，包含未知发布结果。
     */
    private Integer attempts;
    /**
     * 下次允许领取的 UTC 时间。
     */
    private LocalDateTime nextAttemptAt;
    /**
     * 当前领取令牌，防止旧发布者覆盖新结果。
     */
    private String leaseToken;
    /**
     * 发布者崩溃后可重新领取的 UTC 截止时间。
     */
    private LocalDateTime leaseUntil;
    /**
     * 不含地址或凭据的公开失败原因。
     */
    private String lastError;
    /**
     * 事件创建 UTC 时间。
     */
    private LocalDateTime createdAt;
    /**
     * broker 接收确认 UTC 时间；不代表消费成功。
     */
    private LocalDateTime sentAt;
    /**
     * 源订单全局事务 ID；投递领取必须经过 Seata 全局锁检查。
     */
    private String globalXid;
}
