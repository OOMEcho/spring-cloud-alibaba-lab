package cn.oomecho.cloud.stock.controller;

import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.stock.service.AtomicStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局事务库存内部契约；网关不代理 internal 路径，调用必须携带当前事务 XID
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@RestController
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
@Tag(name = "Seata AT 库存分支")
@ApiResponses({@ApiResponse(responseCode = "200", description = "当前全局事务的库存分支结果", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "参数或全局 XID 缺失"),
        @ApiResponse(responseCode = "404", description = "库存不存在"),
        @ApiResponse(responseCode = "409", description = "全局上下文或幂等记录冲突")})
public class AtomicStockController {
    private final AtomicStockService service;

    /**
     * @param service 当前 XID 内的库存事务
     */
    public AtomicStockController(AtomicStockService service) {
        this.service = service;
    }

    /**
     * @param request 与原库存契约一致的购买参数 @return 分支结果，全局提交由订单 TM 决定
     */
    @PostMapping("/internal/stocks/atomic-reservations")
    @Operation(summary = "AT 内部库存扣减", description = "须携带 TX_XID；分支本地提交不代表全局提交")
    @Parameter(name = "TX_XID", in = ParameterIn.HEADER, required = true, description = "由订单 TM 创建并通过 Feign 传递的当前全局事务 ID")
    public Result<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        return Result.success(service.reserve(request));
    }
}
