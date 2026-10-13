package cn.oomecho.cloud.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 订单事件投递状态；不暴露内部 JSON、租约令牌或 RabbitMQ 连接信息
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
public record OrderEventVO(
        @Schema(description = "事件 ID，重投保持不变") String eventId,
        @Schema(description = "订单号") String orderNo,
        @Schema(description = "PENDING/PUBLISHING/SENT/FAILED；SENT 只表示 broker 接收") String status,
        @Schema(description = "发布尝试次数") Integer attempts,
        @Schema(description = "下次尝试 UTC 时间") LocalDateTime nextAttemptAt,
        @Schema(description = "公开失败原因") String lastError) {
}
