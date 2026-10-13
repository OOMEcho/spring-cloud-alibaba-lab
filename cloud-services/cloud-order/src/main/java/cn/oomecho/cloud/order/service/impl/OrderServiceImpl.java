package cn.oomecho.cloud.order.service.impl;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.order.config.LearningProperties;
import cn.oomecho.cloud.order.config.SentinelResources;
import cn.oomecho.cloud.order.domain.dto.OrderPreviewDTO;
import cn.oomecho.cloud.order.domain.vo.OrderPreviewVO;
import cn.oomecho.cloud.order.integration.UserGateway;
import cn.oomecho.cloud.order.service.OrderPreviewEnricher;
import cn.oomecho.cloud.order.service.OrderService;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 订单业务实现层
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final UserGateway userGateway;
    private final LearningProperties properties;
    private final OrderPreviewEnricher enricher;

    /**
     * 创建订单业务实现。
     *
     * @param userGateway 用户远程调用适配层
     * @param properties  动态业务配置
     * @param enricher    当前模式的预览补充实现
     */
    public OrderServiceImpl(UserGateway userGateway, LearningProperties properties, OrderPreviewEnricher enricher) {
        this.userGateway = userGateway;
        this.properties = properties;
        this.enricher = enricher;
    }

    /**
     * 优先进行本地数量校验，再调用用户服务，避免无效请求占用远程资源。
     *
     * @param dto 已通过请求参数校验的预览参数
     * @return 用户校验成功后的预览数据
     */
    @Override
    public OrderPreviewVO preview(OrderPreviewDTO dto) {
        try (Entry entry = SphU.entry(SentinelResources.ORDER_PREVIEW, EntryType.IN)) {
            return doPreview(dto);
        } catch (BlockException exception) {
            throw new BusinessException(429, 42901, "订单请求过于频繁，请稍后重试");
        }
    }

    /**
     * 执行已取得 Sentinel 许可的订单预览，不把限流响应当作业务成功。
     *
     * @param dto 已校验的预览参数
     * @return 用户资格校验后的预览
     */
    private OrderPreviewVO doPreview(OrderPreviewDTO dto) {
        if (dto.quantity() > properties.getMaxQuantity()) {
            throw new BusinessException(400, 40001, "购买数量超过当前上限：" + properties.getMaxQuantity());
        }
        UserResponse user = userGateway.detail(dto.userId());
        if (!user.enabled()) {
            throw new BusinessException(409, 40901, "用户已被禁用，不能下单");
        }
        log.info("订单预览通过，userId={}，productId={}，quantity={}", dto.userId(), dto.productId(), dto.quantity());
        return enricher.enrich(user, dto, properties.getMessage());
    }
}
