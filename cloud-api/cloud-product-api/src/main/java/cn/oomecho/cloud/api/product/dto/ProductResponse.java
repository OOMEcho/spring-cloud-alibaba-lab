package cn.oomecho.cloud.api.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 商品查询契约；价格由商品服务提供，不接受下单客户端传入价格。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "商品信息")
public record ProductResponse(
        @Schema(description = "商品 ID") Long id,
        @Schema(description = "商品名称") String name,
        @Schema(description = "单价，单位元", example = "29.90") BigDecimal price,
        @Schema(description = "是否上架") boolean enabled,
        @Schema(description = "修改版本，用于并发更新校验") Integer version) {
}
