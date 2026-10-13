package cn.oomecho.cloud.order.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 正式下单 DTO；请求号支持重复提交，价格只能由服务端取得
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "正式下单参数；不允许客户端提交价格")
public record OrderCreateDTO(
        @NotBlank @Pattern(regexp = "[a-z0-9_-]{8,64}") @Schema(description = "8-64 位小写 ASCII 请求号；重试沿用原值", example = "order-demo-0001") String requestNo,
        @NotNull @Positive @Schema(description = "用户 ID", example = "1") Long userId,
        @NotNull @Positive @Schema(description = "商品 ID", example = "1001") Long productId,
        @NotNull @Positive @Max(1000) @Schema(description = "购买数量", example = "2") Integer quantity) {
}
