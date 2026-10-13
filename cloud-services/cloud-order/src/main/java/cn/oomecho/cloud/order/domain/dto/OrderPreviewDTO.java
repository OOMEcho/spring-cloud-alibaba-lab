package cn.oomecho.cloud.order.domain.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 订单预览 DTO；正常模式补齐数据库报价和库存快照，不创建订单
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "订单预览参数")
public record OrderPreviewDTO(
        @NotNull @Positive @Schema(description = "用户 ID", example = "1") Long userId,
        @NotNull @Positive @Schema(description = "商品 ID", example = "1001") Long productId,
        @NotNull @Positive @Max(1000) @Schema(description = "购买数量；还受动态业务上限限制", example = "2") Integer quantity) {
}
