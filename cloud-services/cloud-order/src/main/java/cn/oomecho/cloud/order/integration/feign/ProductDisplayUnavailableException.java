package cn.oomecho.cloud.order.integration.feign;

import cn.oomecho.cloud.order.domain.vo.ProductDisplayVO.DegradeReason;

/**
 * 商品展示回退信号；由调用适配层转换为显式降级结果，保留原始故障原因。
 * 不允许用于正式报价、用户资格校验或库存扣减。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
public class ProductDisplayUnavailableException extends RuntimeException {
    /** 对外可解释的降级原因。 */
    private final DegradeReason reason;

    /**
     * @param reason 降级类型
     * @param cause 触发 Feign 回退的原始异常
     */
    public ProductDisplayUnavailableException(DegradeReason reason, Throwable cause) {
        super("商品展示信息暂不可用", cause);
        this.reason = reason;
    }

    /** @return 展示降级类型 */
    public DegradeReason getReason() {
        return reason;
    }
}
