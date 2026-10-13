package cn.oomecho.cloud.order.integration;


import cn.oomecho.cloud.api.stock.client.StockClient;
import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.api.stock.dto.StockResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import feign.FeignException;
import org.springframework.stereotype.Component;

/**
 * 库存调用适配层；超时不能推断扣减失败，必须使用原订单号确认
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Component
public class StockGateway {
    private final StockClient client;

    /**
     * @param client 库存服务契约
     */
    public StockGateway(StockClient client) {
        this.client = client;
    }

    /**
     * @param id 商品 ID
     * @return 有效库存快照
     */
    public StockResponse detail(Long id) {
        try {
            var response = client.detail(id);
            if (response == null || response.code() != 200 || response.data() == null
                    || !id.equals(response.data().productId()) || response.data().available() == null
                    || response.data().available() < 0) {
                throw new BusinessException(502, 50203, "库存服务返回了无效数据");
            }
            return response.data();
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(404, 40404, "该商品尚未配置库存");
        } catch (FeignException exception) {
            throw new BusinessException(503, 50304, "库存服务暂不可用");
        }
    }

    /**
     * @param request 原订单的幂等扣减参数
     * @return 库存已保存的确定结果
     */
    public ReservationResponse reserve(ReservationRequest request) {
        return reserve(request, false);
    }

    /**
     * @param request 原订单参数 @return 当前全局事务中的库存分支结果
     */
    public ReservationResponse reserveAtomic(ReservationRequest request) {
        return reserve(request, true);
    }

    /**
     * @param request 原参数 @param atomic 是否调用 AT 专用入口 @return 校验后的库存结果
     */
    private ReservationResponse reserve(ReservationRequest request, boolean atomic) {
        try {
            var response = atomic ? client.reserveAtomic(request) : client.reserve(request);
            if (response == null || response.code() != 200 || response.data() == null) {
                throw new BusinessException(502, 50203, "库存扣减结果无法确认");
            }
            var result = response.data();
            if (!request.orderNo().equals(result.orderNo()) || !request.productId().equals(result.productId())
                    || !request.quantity().equals(result.quantity())
                    || (!"RESERVED".equals(result.status()) && !"REJECTED".equals(result.status()))) {
                throw new BusinessException(502, 50203, "库存扣减结果与订单不匹配");
            }
            return result;
        } catch (FeignException exception) {
            // 连接/读取失败不能断言库存未提交；上层保存待确认状态后使用同一幂等键重试。
            throw new BusinessException(503, 50304, "库存扣减结果暂无法确认，请使用原订单号重试");
        }
    }
}
