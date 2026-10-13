package cn.oomecho.cloud.order.service;


import cn.oomecho.cloud.order.domain.dto.OrderPreviewDTO;
import cn.oomecho.cloud.order.domain.vo.OrderPreviewVO;

/**
 * 订单业务层
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface OrderService {
    /**
     * 预览购买请求，校验用户状态及数量限制。
     *
     * @param dto 预览参数
     * @return 预览结果；不创建订单
     */
    OrderPreviewVO preview(OrderPreviewDTO dto);
}
