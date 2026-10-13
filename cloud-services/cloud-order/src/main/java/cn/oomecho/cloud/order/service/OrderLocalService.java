package cn.oomecho.cloud.order.service;


import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.domain.entity.Order;
import cn.oomecho.cloud.order.mapper.OrderMapper;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单本地事务层；远程库存调用发生在这些事务提交之后
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Service
public class OrderLocalService {
    private final OrderMapper mapper;
    private final OrderEventRecorder events;
    private final boolean seataEnabled;

    /**
     * @param mapper 订单 Mapper @param events 同事务事件记录器 @param seataEnabled 是否启用全局锁检查
     */
    public OrderLocalService(OrderMapper mapper, OrderEventRecorder events, @Value("${seata.enabled:false}") boolean seataEnabled) {
        this.mapper = mapper;
        this.events = events;
        this.seataEnabled = seataEnabled;
    }

    /**
     * @param orderNo 订单号
     * @return 订单，不存在时为 null
     */
    public Order find(String orderNo) {
        return mapper.selectById(orderNo);
    }

    /**
     * @param orderNo 订单号
     * @return 必须存在的订单
     */
    public Order require(String orderNo) {
        Order order = find(orderNo);
        if (order == null) {
            throw new BusinessException(404, 40403, "订单不存在");
        }
        return order;
    }

    /**
     * @param orderNo 原订单号 @return 全局事务完成后的稳定订单快照
     */
    @GlobalLock(lockRetryTimes = 5, lockRetryInterval = 20)
    @Transactional
    public Order stable(String orderNo) {
        Order row = require(orderNo);
        if (!"GLOBAL".equals(row.getTransactionMode())) return row;
        if (!seataEnabled) throw new BusinessException(503, 50309, "全局事务客户端未启用，暂不能确认该订单");
        row = mapper.locked(orderNo);
        if (row == null) throw new BusinessException(404, 40403, "订单不存在或已全局回滚");
        return row;
    }

    /**
     * @param order 待保存意图
     * @return 首次创建或并发请求已创建的同一订单
     */
    @Transactional
    public Order createIntent(Order order) {
        try {
            mapper.insert(order);
        } catch (DuplicateKeyException duplicate) {
            // 并发相同请求号由唯一键裁决，外层仍需校验原始购买参数。
            return require(order.getOrderNo());
        }
        return require(order.getOrderNo());
    }

    /**
     * @param orderNo  订单号
     * @param reserved 是否已扣库存
     * @return 确认后的订单
     */
    @Transactional
    public Order resolve(String orderNo, boolean reserved) {
        int changed = mapper.resolve(orderNo, reserved ? "CONFIRMED" : "REJECTED", reserved ? null : "库存不足");
        Order order = require(orderNo);
        // 只有首次确认产生事件；订单状态与发件箱由当前同一事务原子提交。
        if (changed == 1 && reserved) events.record(order);
        return order;
    }

    /**
     * @param orderNo 订单号
     * @param error   无法确认的公开原因
     * @return 待确认或其他并发请求已完成的订单
     */
    @Transactional
    public Order markUncertain(String orderNo, String error) {
        mapper.markUncertain(orderNo, error);
        return require(orderNo);
    }
}
