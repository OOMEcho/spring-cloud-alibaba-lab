package cn.oomecho.cloud.api.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 库存扣减结果；RESERVED 已扣减，REJECTED 库存不足，重复请求返回原结果。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "库存扣减结果")
public record ReservationResponse(@Schema(description = "订单号") String orderNo,
                                  @Schema(description = "商品 ID") Long productId,
                                  @Schema(description = "扣减数量") Integer quantity,
                                  @Schema(description = "RESERVED 或 REJECTED") String status) {
}
