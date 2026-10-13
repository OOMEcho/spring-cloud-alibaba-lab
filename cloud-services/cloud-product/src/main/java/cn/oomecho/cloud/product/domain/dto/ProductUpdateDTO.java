package cn.oomecho.cloud.product.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 商品更新 DTO；通过版本号防止覆盖他人更新
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "商品更新参数")
public record ProductUpdateDTO(
        @NotBlank @Size(max = 100) @Schema(description = "商品名称") String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(description = "单价，单位元") BigDecimal price,
        @NotNull @Schema(description = "是否上架") Boolean enabled,
        @NotNull @PositiveOrZero @Schema(description = "查询结果中的当前版本") Integer version) {
}
