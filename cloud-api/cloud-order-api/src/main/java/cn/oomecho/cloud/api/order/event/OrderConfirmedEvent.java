package cn.oomecho.cloud.api.order.event;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 订单确认事件 V1；只共享不可变消息契约，不共享订单实体或 Mapper
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Schema(description = "订单确认事件，重复投递保持 eventId 和内容不变")
public record OrderConfirmedEvent(
        @NotBlank @Pattern(regexp = "[a-f0-9-]{36}") @Schema(description = "事件唯一 ID") String eventId,
        @NotBlank @Pattern(regexp = "ORDER_CONFIRMED") @Schema(description = "固定事件类型") String eventType,
        @Min(1) @Max(1) @Schema(description = "契约版本，当前为 1") int version,
        @NotBlank @Schema(description = "事件发生时间，UTC ISO-8601") String occurredAt,
        @NotBlank @Pattern(regexp = "[a-z0-9_-]{8,64}") @Schema(description = "原订单号") String orderNo,
        @NotNull @Positive @Schema(description = "用户 ID") Long userId,
        @NotNull @Positive @Schema(description = "商品 ID") Long productId,
        @NotBlank @Size(max = 100) @Schema(description = "下单时商品名称") String productName,
        @NotNull @Positive @Schema(description = "购买数量") Integer quantity,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(description = "下单单价，元") BigDecimal unitPrice,
        @NotNull @DecimalMin("0.01") @Digits(integer = 14, fraction = 2) @Schema(description = "订单金额，元") BigDecimal totalAmount) {
}
