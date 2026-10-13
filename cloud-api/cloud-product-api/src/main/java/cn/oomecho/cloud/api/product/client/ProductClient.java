package cn.oomecho.cloud.api.product.client;

import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品服务调用契约；下单报价直接读取数据库，独立于展示缓存。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@FeignClient(name = "cloud-product", contextId = "productClient", url = "${clients.product.url:}")
public interface ProductClient {
    /**
     * 获取下单所需的当前商品价格和状态。
     *
     * @param id 商品 ID
     * @return 数据库中的商品报价
     */
    @GetMapping("/internal/products/{id}/quote")
    Result<ProductResponse> quote(@PathVariable("id") Long id);
}
