package cn.oomecho.cloud.stock.service;


import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.api.stock.dto.StockResponse;

/**
 * 库存业务层；查询只是快照，扣减通过原子更新确认
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface StockService {
    /**
     * @param id 商品 ID
     * @return 当前可用数量
     */
    StockResponse detail(Long id);

    /**
     * @param request 幂等扣减请求
     * @return 首次结果或之前保存的同一结果
     */
    ReservationResponse reserve(ReservationRequest request);
}
