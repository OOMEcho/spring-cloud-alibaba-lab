package cn.oomecho.cloud.order.service.impl;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.domain.dto.OrderPreviewDTO;
import cn.oomecho.cloud.order.domain.vo.OrderPreviewVO;
import cn.oomecho.cloud.order.integration.ProductGateway;
import cn.oomecho.cloud.order.integration.StockGateway;
import cn.oomecho.cloud.order.service.OrderPreviewEnricher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 正常订单预览；商品报价直接读取数据库，库存仍只是实时查询快照
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class DatabasePreviewEnricher implements OrderPreviewEnricher {
    private final ProductGateway products;
    private final StockGateway stocks;

    /**
     * @param products 商品报价适配器
     * @param stocks   库存查询适配器
     */
    public DatabasePreviewEnricher(ProductGateway products, StockGateway stocks) {
        this.products = products;
        this.stocks = stocks;
    }

    /**
     * @param user    已校验用户
     * @param dto     购买参数
     * @param message 当前文案
     * @return 数据库报价和库存快照
     */
    @Override
    public OrderPreviewVO enrich(UserResponse user, OrderPreviewDTO dto, String message) {
        var product = products.quote(dto.productId());
        var stock = stocks.detail(dto.productId());
        if (stock.available() < dto.quantity()) {
            throw new BusinessException(409, 40902, "库存不足");
        }
        return new OrderPreviewVO(user.id(), user.nickname(), dto.productId(), dto.quantity(), message,
                product.name(), product.price(), product.price().multiply(BigDecimal.valueOf(dto.quantity())), stock.available());
    }
}
