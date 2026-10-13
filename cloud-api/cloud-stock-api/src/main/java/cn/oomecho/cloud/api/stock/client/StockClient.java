package cn.oomecho.cloud.api.stock.client;

import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.api.stock.dto.StockResponse;
import cn.oomecho.cloud.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务调用契约；写请求通过订单号提供业务幂等能力。
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@FeignClient(name = "cloud-stock", contextId = "stockClient", url = "${clients.stock.url:}")
public interface StockClient {
    /**
     * @param id 商品 ID
     * @return 当前可用库存
     */
    @GetMapping("/stocks/{id}")
    Result<StockResponse> detail(@PathVariable("id") Long id);

    /**
     * @param request 扣减参数
     * @return 首次处理或之前已保存的扣减结果
     */
    @PostMapping("/internal/stocks/reservations")
    Result<ReservationResponse> reserve(@RequestBody ReservationRequest request);

    /**
     * @param request 原订单扣减参数 @return Seata AT 分支扣减结果，必须携带全局 XID
     */
    @PostMapping("/internal/stocks/atomic-reservations")
    Result<ReservationResponse> reserveAtomic(@RequestBody ReservationRequest request);
}
