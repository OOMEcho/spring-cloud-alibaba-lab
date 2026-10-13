package cn.oomecho.cloud.order.service;

import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.config.LearningProperties;
import cn.oomecho.cloud.order.domain.dto.OrderCreateDTO;
import cn.oomecho.cloud.order.domain.entity.Order;
import cn.oomecho.cloud.order.domain.vo.OrderVO;
import cn.oomecho.cloud.order.integration.ProductGateway;
import cn.oomecho.cloud.order.integration.StockGateway;
import cn.oomecho.cloud.order.integration.UserGateway;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Seata AT 同步下单编排；订单意图、库存分支、订单确认与发件箱属于同一个全局事务
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
public class AtomicOrderService {
    private static final Logger log = LoggerFactory.getLogger(AtomicOrderService.class);
    private final OrderLocalService local;
    private final OrderConvert convert;
    private final UserGateway users;
    private final ProductGateway products;
    private final StockGateway stocks;
    private final LearningProperties properties;

    /**
     * @param local 本地分支事务 @param convert 响应映射 @param users 用户校验 @param products 数据库报价 @param stocks 库存分支 @param properties 数量限制
     */
    public AtomicOrderService(OrderLocalService local, OrderConvert convert, UserGateway users, ProductGateway products,
                              StockGateway stocks, LearningProperties properties) {
        this.local = local;
        this.convert = convert;
        this.users = users;
        this.products = products;
        this.stocks = stocks;
        this.properties = properties;
    }

    /**
     * @param dto 与普通下单一致的请求号和购买参数
     * @return 全局提交前生成的订单快照；调用方须经过代理的全局提交后再返回 HTTP 成功
     */
    @GlobalTransactional(name = "lab-order-atomic", timeoutMills = 30000, rollbackFor = Exception.class)
    public OrderVO create(OrderCreateDTO dto) {
        String xid = RootContext.getXID();
        if (xid == null) throw new IllegalStateException("Seata 全局事务拦截未生效");
        Order order = local.find(dto.requestNo());
        if (order != null) {
            order = local.stable(dto.requestNo());
            check(order, dto);
            if ("CONFIRMED".equals(order.getStatus())) return convert.toVO(order);
            throw new BusinessException(409, 40909, "该全局事务订单尚在处理中，请沿用原号稍后查询");
        }
        if (dto.quantity() > properties.getMaxQuantity())
            throw new BusinessException(400, 40001, "购买数量超过当前上限");
        var user = users.detail(dto.userId());
        if (!user.enabled()) throw new BusinessException(409, 40901, "用户已被禁用，不能下单");
        var product = products.quote(dto.productId());
        order = new Order();
        order.setOrderNo(dto.requestNo());
        order.setUserId(dto.userId());
        order.setProductId(dto.productId());
        order.setProductName(product.name());
        order.setUnitPrice(product.price());
        order.setQuantity(dto.quantity());
        order.setTotalAmount(product.price().multiply(BigDecimal.valueOf(dto.quantity())));
        order.setStatus("PENDING_STOCK");
        order.setTransactionMode("GLOBAL");
        order.setGlobalXid(xid);
        order = local.createIntent(order);
        check(order, dto);
        if (!xid.equals(order.getGlobalXid())) {
            // 并发相同号由唯一键裁决，不能接管另一个未完成的全局事务分支。
            if ("CONFIRMED".equals(local.stable(order.getOrderNo()).getStatus()))
                return convert.toVO(local.stable(order.getOrderNo()));
            throw new BusinessException(409, 40909, "同号全局事务尚未完成，请稍后查询");
        }
        log.info("AT 订单意图已提交分支，orderNo={}，xid={}", order.getOrderNo(), xid);
        var result = stocks.reserveAtomic(new ReservationRequest(order.getOrderNo(), order.getProductId(), order.getQuantity()));
        if (!"RESERVED".equals(result.status())) throw new BusinessException(409, 40902, "库存不足，全局下单未完成");
        // 异常必须离开全局代理，触发 TC 回滚；不能保存本地待确认后假称整体成功。
        var confirmed = local.resolve(order.getOrderNo(), true);
        log.info("AT 订单确认分支已提交，等待全局提交，orderNo={}，xid={}", order.getOrderNo(), xid);
        return convert.toVO(confirmed);
    }

    /**
     * @param order 原持久化订单 @param dto 本次购买参数
     */
    private void check(Order order, OrderCreateDTO dto) {
        if (!"GLOBAL".equals(order.getTransactionMode()))
            throw new BusinessException(409, 40908, "请求号已属于普通下单流程，不能切换事务模式");
        if (!order.getUserId().equals(dto.userId()) || !order.getProductId().equals(dto.productId()) || !order.getQuantity().equals(dto.quantity()))
            throw new BusinessException(409, 40906, "请求号已经用于其他下单参数");
    }
}
