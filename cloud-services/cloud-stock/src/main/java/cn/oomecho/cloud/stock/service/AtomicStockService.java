package cn.oomecho.cloud.stock.service;

import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.stock.domain.entity.Reservation;
import cn.oomecho.cloud.stock.mapper.ReservationMapper;
import cn.oomecho.cloud.stock.mapper.StockMapper;
import org.apache.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AT 库存分支；本地提交自动登记分支及 undo_log，RootContext XID 必须由请求入口绑定
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@Service
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
public class AtomicStockService {
    private static final Logger log = LoggerFactory.getLogger(AtomicStockService.class);
    private final StockMapper stocks;
    private final ReservationMapper reservations;
    private final StockConvert convert;

    /**
     * @param stocks 库存 Mapper @param reservations 幂等 Mapper @param convert 响应映射
     */
    public AtomicStockService(StockMapper stocks, ReservationMapper reservations, StockConvert convert) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.convert = convert;
    }

    /**
     * @param request 原订单号与扣减参数 @return 当前全局事务分支处理结果
     */
    @Transactional
    public ReservationResponse reserve(ReservationRequest request) {
        String xid = RootContext.getXID();
        if (xid == null) throw new BusinessException(400, 40002, "AT 扣减必须携带全局事务 XID");
        var row = reservations.locked(request.orderNo());
        if (row != null) {
            if (!"GLOBAL".equals(row.getTransactionMode()) || !xid.equals(row.getGlobalXid()))
                throw new BusinessException(409, 40910, "库存请求号已属于其他事务");
            if (!row.getProductId().equals(request.productId()) || !row.getQuantity().equals(request.quantity()))
                throw new BusinessException(409, 40905, "库存请求号已用于其他商品或数量");
            return convert.toResponse(row);
        }
        row = new Reservation();
        row.setOrderNo(request.orderNo());
        row.setProductId(request.productId());
        row.setQuantity(request.quantity());
        row.setStatus("PROCESSING");
        row.setTransactionMode("GLOBAL");
        row.setGlobalXid(xid);
        // 使用普通 INSERT 生成准确 AT 前后镜像，重复号不能覆盖别的事务记录。
        reservations.insert(row);
        int changed = stocks.decrease(request.productId(), request.quantity());
        if (changed == 0 && stocks.selectById(request.productId()) == null)
            throw new BusinessException(404, 40404, "该商品尚未配置库存");
        row.setStatus(changed == 1 ? "RESERVED" : "REJECTED");
        reservations.updateById(row);
        log.info("AT 库存分支写入，orderNo={}，xid={}，status={}", request.orderNo(), xid, row.getStatus());
        return convert.toResponse(row);
    }
}
