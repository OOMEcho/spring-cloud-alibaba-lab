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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 正式下单业务编排；保存意图、幂等扣库存、保存结果，支持未知结果重试
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Service
public class OrderWriteService {
    private static final Logger log = LoggerFactory.getLogger(OrderWriteService.class);
    private final OrderLocalService local;
    private final OrderConvert convert;
    private final UserGateway users;
    private final ProductGateway products;
    private final StockGateway stocks;
    private final LearningProperties properties;

    /**
     * @param local      本地事务服务
     * @param convert    类型映射
     * @param users      用户调用
     * @param products   商品报价
     * @param stocks     库存确认
     * @param properties 数量限制
     */
    public OrderWriteService(OrderLocalService local, OrderConvert convert, UserGateway users,
                             ProductGateway products, StockGateway stocks, LearningProperties properties) {
        this.local = local;
        this.convert = convert;
        this.users = users;
        this.products = products;
        this.stocks = stocks;
        this.properties = properties;
    }

    /**
     * @param dto 正式下单参数
     * @return 首次或重复提交的已确认订单
     */
    public OrderVO create(OrderCreateDTO dto) {
        Order order = local.find(dto.requestNo());
        if (order == null) {
            if (dto.quantity() > properties.getMaxQuantity()) {
                throw new BusinessException(400, 40001, "购买数量超过当前上限");
            }
            var user = users.detail(dto.userId());
            if (!user.enabled()) {
                throw new BusinessException(409, 40901, "用户已被禁用，不能下单");
            }
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
            // 方法返回时本地事务已提交，进程异常后仍能按订单号恢复。
            order = local.createIntent(order);
            log.info("订单意图已保存，orderNo={}，status={}", order.getOrderNo(), order.getStatus());
        }
        if (!"LOCAL".equals(order.getTransactionMode())) {
            throw new BusinessException(409, 40908, "该请求号属于全局事务订单，请使用全局事务入口");
        }
        if (!order.getUserId().equals(dto.userId()) || !order.getProductId().equals(dto.productId())
                || !order.getQuantity().equals(dto.quantity())) {
            throw new BusinessException(409, 40906, "请求号已经用于其他下单参数");
        }
        return confirm(order);
    }

    /**
     * @param orderNo 已保存订单号
     * @return 数据库中的订单状态
     */
    public OrderVO detail(String orderNo) {
        return convert.toVO(local.stable(orderNo));
    }

    /**
     * @param orderNo 原订单号
     * @return 原参数确认后的订单，不重新计算价格或生成订单号
     */
    public OrderVO retry(String orderNo) {
        Order order = local.require(orderNo);
        if (!"LOCAL".equals(order.getTransactionMode()))
            throw new BusinessException(409, 40908, "全局事务订单请通过全局事务下单入口沿用原号确认");
        return confirm(order);
    }

    /**
     * @param order 已提交的订单意图
     * @return 确定成功的订单，拒绝或未知结果返回明确错误
     */
    private OrderVO confirm(Order order) {
        if ("CONFIRMED".equals(order.getStatus())) {
            return convert.toVO(order);
        }
        if ("REJECTED".equals(order.getStatus())) {
            throw new BusinessException(409, 40902, "库存不足，该订单已拒绝");
        }
        if (!"PENDING_STOCK".equals(order.getStatus()) && !"RECONCILE_REQUIRED".equals(order.getStatus())) {
            throw new IllegalStateException("订单状态异常");
        }
        boolean reserved;
        try {
            var result = stocks.reserve(new ReservationRequest(order.getOrderNo(), order.getProductId(), order.getQuantity()));
            reserved = "RESERVED".equals(result.status());
        } catch (BusinessException exception) {
            Order latest = local.markUncertain(order.getOrderNo(), exception.getMessage());
            log.warn("库存结果待确认，orderNo={}，status={}", order.getOrderNo(), latest.getStatus());
            // 另一并发重试已完成时优先返回持久化的确定结果，不退回未知状态。
            if ("CONFIRMED".equals(latest.getStatus())) {
                return convert.toVO(latest);
            }
            if ("REJECTED".equals(latest.getStatus())) {
                throw new BusinessException(409, 40902, "库存不足，该订单已拒绝");
            }
            throw exception;
        }
        Order resolved = local.resolve(order.getOrderNo(), reserved);
        log.info("订单库存结果已保存，orderNo={}，status={}", resolved.getOrderNo(), resolved.getStatus());
        if ("REJECTED".equals(resolved.getStatus())) {
            throw new BusinessException(409, 40902, "库存不足，该订单已拒绝");
        }
        return convert.toVO(resolved);
    }
}
