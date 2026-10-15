package cn.oomecho.cloud.order.integration.feign;

import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 订单侧商品展示客户端；回退策略只属于本消费者，不放入共享 API 模块。
 * 展示查询可使用商品服务缓存，正式下单仍由 ProductClient 查询数据库报价。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
@FeignClient(name = "cloud-product", contextId = "productDisplayClient", url = "${clients.product.url:}",
        configuration = ProductDisplayFeignConfiguration.class, fallbackFactory = ProductDisplayFallbackFactory.class)
public interface ProductDisplayClient {
    /**
     * 查询商品展示信息；正常运行通过服务发现访问商品服务。
     *
     * @param id 商品 ID
     * @return 商品服务的展示响应
     */
    @GetMapping("/products/{id}")
    Result<ProductResponse> detail(@PathVariable("id") Long id);
}
