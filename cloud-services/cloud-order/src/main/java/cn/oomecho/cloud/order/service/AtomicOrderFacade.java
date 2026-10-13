package cn.oomecho.cloud.order.service;

import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.domain.dto.OrderCreateDTO;
import cn.oomecho.cloud.order.domain.vo.OrderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 全局代理外的 HTTP 业务边界；确认全局提交完成后读取稳定结果，基础设施故障不假称回滚成功
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
public class AtomicOrderFacade {
    private static final Logger log = LoggerFactory.getLogger(AtomicOrderFacade.class);
    private final AtomicOrderService atomic;
    private final OrderWriteService orders;

    /**
     * @param atomic 独立 Bean 的全局事务代理 @param orders 稳定查询
     */
    public AtomicOrderFacade(AtomicOrderService atomic, OrderWriteService orders) {
        this.atomic = atomic;
        this.orders = orders;
    }

    /**
     * @param dto 全局下单参数 @return 已经通过全局提交与锁检查的订单
     */
    public OrderVO create(OrderCreateDTO dto) {
        try {
            var result = atomic.create(dto);
            return orders.detail(result.orderNo());
        } catch (BusinessException business) {
            throw business;
        } catch (RuntimeException failure) {
            log.warn("全局下单结果需查询，orderNo={}，errorType={}", dto.requestNo(), failure.getClass().getSimpleName());
            throw new BusinessException(503, 50310, "全局下单结果暂未确认，请保留原请求号查询后再确认");
        }
    }
}
