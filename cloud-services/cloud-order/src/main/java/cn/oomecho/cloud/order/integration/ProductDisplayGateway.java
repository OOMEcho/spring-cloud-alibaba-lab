package cn.oomecho.cloud.order.integration;

import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.domain.vo.ProductDisplayVO;
import cn.oomecho.cloud.order.domain.vo.ProductDisplayVO.DegradeReason;
import cn.oomecho.cloud.order.integration.feign.ProductDisplayClient;
import cn.oomecho.cloud.order.integration.feign.ProductDisplayUnavailableException;
import org.springframework.stereotype.Component;

/**
 * 商品展示调用适配层；只将明确的可降级故障转换为部分信息结果。
 * 正式下单的 ProductGateway 不依赖本类，也不读取本类的展示价格。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
@Component
public class ProductDisplayGateway {
    /** 带消费者专属回退策略的展示客户端。 */
    private final ProductDisplayClient client;

    /** @param client 商品展示客户端 */
    public ProductDisplayGateway(ProductDisplayClient client) {
        this.client = client;
    }

    /**
     * 查询可选展示信息；正常响应仍严格校验，不能把契约错误当作降级。
     *
     * @param id 商品 ID
     * @return 正常或显式降级的展示结果
     */
    public ProductDisplayVO detail(Long id) {
        try {
            var response = client.detail(id);
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(502, 50210, "商品服务返回了无效展示数据");
            }
            var product = response.data();
            if (!id.equals(product.id()) || product.name() == null || product.name().isBlank()
                    || product.price() == null || product.price().signum() <= 0) {
                throw new BusinessException(502, 50210, "商品服务返回了无效展示数据");
            }
            return new ProductDisplayVO(id, product.name(), product.price(), product.enabled(), false, DegradeReason.NONE);
        } catch (ProductDisplayUnavailableException exception) {
            return new ProductDisplayVO(id, null, null, null, true, exception.getReason());
        }
    }
}
