package cn.oomecho.cloud.notification.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 已创建的站内订单通知；保存记录不代表邮件、短信或第三方消息已送达
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
public record OrderNotificationVO(
        @Schema(description = "源事件 ID") String eventId,
        @Schema(description = "订单号") String orderNo,
        @Schema(description = "用户 ID") Long userId,
        @Schema(description = "站内通知内容") String content,
        @Schema(description = "原订单金额，元") BigDecimal totalAmount,
        @Schema(description = "通知创建 UTC 时间") LocalDateTime createdAt) {
}
