package cn.oomecho.cloud.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 订单预览 VO；正常模式提供数据库报价和库存快照，不创建订单。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "订单预览结果；库存为查询快照，正式下单仍需原子扣减")
public record OrderPreviewVO(
        @Schema(description = "用户 ID") Long userId,
        @Schema(description = "用户昵称") String nickname,
        @Schema(description = "商品 ID") Long productId,
        @Schema(description = "购买数量") Integer quantity,
        @Schema(description = "当前配置文案") String message,
        @Schema(description = "商品名称") String productName,
        @Schema(description = "商品数据库单价，单位元") BigDecimal unitPrice,
        @Schema(description = "预览金额，单位元") BigDecimal totalAmount,
        @Schema(description = "可用库存快照") Integer availableStock) {
}
