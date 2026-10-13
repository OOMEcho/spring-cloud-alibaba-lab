package cn.oomecho.cloud.order.service;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.order.domain.dto.OrderPreviewDTO;
import cn.oomecho.cloud.order.domain.vo.OrderPreviewVO;

/**
 * 订单预览补充接口；查询商品数据库报价和库存快照
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface OrderPreviewEnricher {
    /**
     * @param user    已校验用户
     * @param dto     购买参数
     * @param message 当前文案
     * @return 包含商品与库存信息的预览
     */
    OrderPreviewVO enrich(UserResponse user, OrderPreviewDTO dto, String message);
}
