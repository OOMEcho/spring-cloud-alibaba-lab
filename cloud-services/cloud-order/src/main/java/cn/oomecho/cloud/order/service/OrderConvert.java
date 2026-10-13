package cn.oomecho.cloud.order.service;


import cn.oomecho.cloud.order.domain.entity.Order;
import cn.oomecho.cloud.order.domain.vo.OrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 订单实体到查询 VO 的 MapStruct 映射
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderConvert {
    /**
     * @param order 数据库订单
     * @return 订单响应
     */
    OrderVO toVO(Order order);
}
