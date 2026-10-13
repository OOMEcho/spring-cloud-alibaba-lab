package cn.oomecho.cloud.order.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 可动态刷新的业务配置；通过接口与订单预览验证真实生效
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
@RefreshScope
public class LearningProperties {
    /**
     * 用于观察 Nacos 配置刷新是否生效的文案。
     */
    private final String message;
    /**
     * 当前允许的单次购买数量上限。
     */
    private final int maxQuantity;

    /**
     * 从当前环境读取学习配置，Nacos 刷新后重新创建该实例。
     *
     * @param message     配置验证文案
     * @param maxQuantity 单次预览最大购买数量
     */
    public LearningProperties(@Value("${lab.message:本地默认配置}") String message,
                              @Value("${lab.order.max-quantity:10}") int maxQuantity) {
        if (maxQuantity < 1 || maxQuantity > 1000) {
            throw new IllegalArgumentException("lab.order.max-quantity 必须在 1 到 1000 之间");
        }
        this.message = message;
        this.maxQuantity = maxQuantity;
    }

    /**
     * @return 当前配置文案
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return 单次最大购买数量
     */
    public int getMaxQuantity() {
        return maxQuantity;
    }
}
