package cn.oomecho.cloud.order.integration;


import cn.oomecho.cloud.api.product.client.ProductClient;
import cn.oomecho.cloud.api.product.dto.ProductResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import feign.FeignException;
import org.springframework.stereotype.Component;

/**
 * 商品服务调用适配层；只使用数据库报价，不信任客户端价格
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class ProductGateway {
    private final ProductClient client;

    /**
     * @param client 商品服务契约
     */
    public ProductGateway(ProductClient client) {
        this.client = client;
    }

    /**
     * @param id 商品 ID
     * @return 已验证上架状态的数据库商品报价
     */
    public ProductResponse quote(Long id) {
        ProductResponse product;
        try {
            var response = client.quote(id);
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(502, 50202, "商品服务返回了无效报价");
            }
            product = response.data();
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(404, 40402, "商品不存在");
        } catch (FeignException exception) {
            throw new BusinessException(503, 50303, "商品服务暂不可用");
        }
        if (!id.equals(product.id()) || product.price() == null || product.price().signum() <= 0) {
            throw new BusinessException(502, 50202, "商品服务返回了无效报价");
        }
        if (!product.enabled()) {
            throw new BusinessException(409, 40904, "商品已下架，不能下单");
        }
        return product;
    }
}
