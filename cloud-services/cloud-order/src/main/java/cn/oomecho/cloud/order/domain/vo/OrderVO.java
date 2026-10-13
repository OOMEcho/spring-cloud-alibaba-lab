package cn.oomecho.cloud.order.domain.vo;


import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单查询 VO；CONFIRMED 表示已扣库存，不代表已付款或发货
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "持久化订单信息")
public record OrderVO(
        @Schema(description = "订单号，等于 requestNo") String orderNo,
        @Schema(description = "用户 ID") Long userId,
        @Schema(description = "商品 ID") Long productId,
        @Schema(description = "商品名称快照") String productName,
        @Schema(description = "单价快照，单位元") BigDecimal unitPrice,
        @Schema(description = "购买数量") Integer quantity,
        @Schema(description = "服务端计算的订单金额，单位元") BigDecimal totalAmount,
        @Schema(description = "订单状态") String status,
        @Schema(description = "需确认或拒绝的原因") String lastError,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt,
        @Schema(description = "LOCAL 或 GLOBAL，禁止同号跨模式重试") String transactionMode,
        @Schema(description = "GLOBAL 模式对应的 Seata XID") String globalXid) {
}
