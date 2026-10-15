package cn.oomecho.cloud.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 可降级的商品展示结果；降级时省略远程字段，不提供可用于下单的替代报价。
 *
 * @param productId 请求的商品 ID；降级时不代表已确认商品存在
 * @param name 商品名称；降级时为空
 * @param displayPrice 展示参考价格；降级时为空，不能用于正式下单
 * @param enabled 展示上架状态；降级时为空，不能默认允许购买
 * @param degraded 是否发生展示降级
 * @param reason 降级原因；正常为 NONE
 * @author xuesong.lei
 * @since 2026-10-14
 */
@Schema(description = "商品展示参考信息；degraded=true 时不能据此判断商品存在或允许购买")
public record ProductDisplayVO(
        @Schema(description = "请求的商品 ID") Long productId,
        @Schema(description = "商品名称；降级时为空") String name,
        @Schema(description = "展示参考价格；降级时为空，正式下单重新取得数据库报价") BigDecimal displayPrice,
        @Schema(description = "上架状态；降级时为空") Boolean enabled,
        @Schema(description = "是否发生降级") boolean degraded,
        @Schema(description = "NONE、TIMEOUT、UNAVAILABLE、CIRCUIT_OPEN 或 RATE_LIMITED") DegradeReason reason) {
    /** 展示结果的降级原因；不向调用方泄露原始异常或内部服务地址。 */
    public enum DegradeReason {
        /** 远程查询正常完成。 */
        NONE,
        /** 连接或读取超时。 */
        TIMEOUT,
        /** 依赖无法连接或返回 5xx。 */
        UNAVAILABLE,
        /** Sentinel 熔断器打开，本次未发起远程请求。 */
        CIRCUIT_OPEN,
        /** Sentinel 限流或依赖返回 429。 */
        RATE_LIMITED
    }
}
