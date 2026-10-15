package cn.oomecho.cloud.order.integration.feign;

import com.alibaba.cloud.sentinel.feign.SentinelFeign;
import feign.Feign;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * 仅商品展示客户端使用的 Sentinel Feign 配置。
 * 不添加 Configuration 注解，避免被主应用扫描为全部客户端的默认配置。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
public class ProductDisplayFeignConfiguration {
    /**
     * 每个客户端构建过程使用独立 Builder，启用 Sentinel 与 FallbackFactory 集成。
     *
     * Sentinel 2025.0.0.0 会从主应用读取 FeignClientFactoryBean 元数据。
     * FactoryBean 返回的对象不再次执行 ApplicationContextAware，避免其上下文被换成子上下文。
     *
     * @param context 当前 Feign 子上下文
     * @return 只为本客户端生产 Builder 的工厂
     */
    @Bean
    @Scope("prototype")
    public FactoryBean<Feign.Builder> productDisplayFeignBuilder(ApplicationContext context) {
        return new FactoryBean<>() {
            /** @return 已绑定主应用上下文的 Sentinel 构建器 */
            @Override
            public Feign.Builder getObject() {
                SentinelFeign.Builder builder = SentinelFeign.builder();
                builder.setApplicationContext(context.getParent());
                return builder;
            }

            /** @return Feign 按此类型查找构建器 */
            @Override
            public Class<?> getObjectType() {
                return Feign.Builder.class;
            }

            /** @return 不共享可变 Builder */
            @Override
            public boolean isSingleton() {
                return false;
            }
        };
    }
}
