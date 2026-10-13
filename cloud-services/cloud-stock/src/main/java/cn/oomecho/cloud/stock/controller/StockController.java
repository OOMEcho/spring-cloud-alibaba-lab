package cn.oomecho.cloud.stock.controller;


import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.api.stock.dto.ReservationResponse;
import cn.oomecho.cloud.api.stock.dto.StockResponse;
import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.stock.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 库存查询和内部幂等扣减接口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestController
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "请求已处理，具体业务结果见 data", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "商品尚未配置库存"),
        @ApiResponse(responseCode = "409", description = "幂等键与原始参数冲突"),
        @ApiResponse(responseCode = "500", description = "内部服务错误")
})
@Tag(name = "库存接口")
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class StockController {
    private final StockService service;

    /**
     * @param service 库存业务层
     */
    public StockController(StockService service) {
        this.service = service;
    }

    /**
     * @param id 商品 ID
     * @return 可用库存快照
     */
    @GetMapping("/stocks/{id}")
    @Operation(summary = "库存详情")
    public Result<StockResponse> detail(@PathVariable @Positive Long id) {
        return Result.success(service.detail(id));
    }

    /**
     * @param request 扣减参数
     * @return 已持久化的幂等扣减结果
     */
    @PostMapping("/internal/stocks/reservations")
    @Operation(summary = "幂等扣减库存", description = "同订单号相同参数返回原结果；库存不足返回 data.status=REJECTED，网关不代理此 internal 路径")
    public Result<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        return Result.success(service.reserve(request));
    }
}
