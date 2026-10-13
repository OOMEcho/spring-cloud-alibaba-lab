package cn.oomecho.cloud.api.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 库存扣减请求；同一个订单号只允许扣减一次，相同订单号不能更换商品或数量。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "幂等库存扣减请求")
public record ReservationRequest(
        @NotBlank @Pattern(regexp = "[a-z0-9_-]{8,64}") @Schema(description = "小写 ASCII 订单号，也是扣减幂等键") String orderNo,
        @NotNull @Positive @Schema(description = "商品 ID") Long productId,
        @NotNull @Positive @Max(1000) @Schema(description = "扣减数量") Integer quantity) {
}
