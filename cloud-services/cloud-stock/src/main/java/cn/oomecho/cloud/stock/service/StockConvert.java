package cn.oomecho.cloud.stock.service;


import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.api.stock.dto.StockResponse;
import cn.oomecho.cloud.stock.domain.entity.Reservation;
import cn.oomecho.cloud.stock.domain.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 库存 Entity 到服务契约的 MapStruct 映射
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StockConvert {
    /**
     * @param stock 库存实体
     * @return 库存查询响应
     */
    StockResponse toResponse(Stock stock);

    /**
     * @param reservation 扣减记录
     * @return 已保存的扣减结果
     */
    ReservationResponse toResponse(Reservation reservation);
}
