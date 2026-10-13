package cn.oomecho.cloud.api.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 库存查询契约；查询结果仅用于展示，最终扣减以条件更新为准。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "库存信息")
public record StockResponse(@Schema(description = "商品 ID") Long productId,
                            @Schema(description = "当前可用数量") Integer available) {
}
