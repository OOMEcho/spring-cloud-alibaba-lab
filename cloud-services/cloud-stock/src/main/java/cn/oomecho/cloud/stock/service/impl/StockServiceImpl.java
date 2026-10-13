package cn.oomecho.cloud.stock.service.impl;


import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.api.stock.dto.StockResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.stock.domain.entity.Reservation;
import cn.oomecho.cloud.stock.mapper.ReservationMapper;
import cn.oomecho.cloud.stock.mapper.StockMapper;
import cn.oomecho.cloud.stock.service.StockConvert;
import cn.oomecho.cloud.stock.service.StockService;
import org.apache.seata.spring.annotation.GlobalLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存业务实现层；条件扣减和幂等结果在同一个数据库事务中提交
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Service
public class StockServiceImpl implements StockService {
    private final StockMapper stocks;
    private final ReservationMapper reservations;
    private final StockConvert convert;

    /**
     * @param stocks       库存 Mapper
     * @param reservations 幂等记录 Mapper
     * @param convert      类型映射
     */
    public StockServiceImpl(StockMapper stocks, ReservationMapper reservations, StockConvert convert) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.convert = convert;
    }

    /**
     * @param id 商品 ID
     * @return 库存查询快照
     */
    @Override
    public StockResponse detail(Long id) {
        var stock = stocks.selectById(id);
        if (stock == null) {
            throw new BusinessException(404, 40404, "该商品尚未配置库存");
        }
        return convert.toResponse(stock);
    }

    /**
     * @param request 已通过参数校验的请求
     * @return 已扣减或库存不足的持久化结果
     */
    @Override
    @GlobalLock
    @Transactional
    public ReservationResponse reserve(ReservationRequest request) {
        int claimed;
        try {
            claimed = reservations.claim(request);
        } catch (DuplicateKeyException duplicate) {
            claimed = 0;
        }
        Reservation reservation = reservations.locked(request.orderNo());
        if (reservation == null) {
            throw new IllegalStateException("幂等记录未创建");
        }
        if (!"LOCAL".equals(reservation.getTransactionMode()))
            throw new BusinessException(409, 40910, "该库存记录属于全局事务，不能改用本地扣减");
        if (!reservation.getProductId().equals(request.productId()) || !reservation.getQuantity().equals(request.quantity())) {
            throw new BusinessException(409, 40905, "订单号已经用于其他商品或数量");
        }
        if (claimed == 0) {
            if (!"RESERVED".equals(reservation.getStatus()) && !"REJECTED".equals(reservation.getStatus())) {
                throw new IllegalStateException("库存幂等记录状态异常");
            }
            return convert.toResponse(reservation);
        }
        // 不能先查数量再无条件减：并发请求必须在数据库中竞争同一条件更新。
        int changed = stocks.decrease(request.productId(), request.quantity());
        if (changed == 0 && stocks.selectById(request.productId()) == null) {
            throw new BusinessException(404, 40404, "该商品尚未配置库存");
        }
        reservation.setStatus(changed == 1 ? "RESERVED" : "REJECTED");
        reservations.updateById(reservation);
        return convert.toResponse(reservation);
    }
}
