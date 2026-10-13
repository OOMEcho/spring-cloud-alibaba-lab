package cn.oomecho.cloud.order.controller;


import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.domain.dto.OrderCreateDTO;
import cn.oomecho.cloud.order.domain.vo.OrderVO;
import cn.oomecho.cloud.order.service.OrderWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 正式下单、订单查询与原订单重试接口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestController
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "请求已处理，具体业务结果见 data", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "参数或数量限制错误"),
        @ApiResponse(responseCode = "404", description = "用户商品或订单不存在"),
        @ApiResponse(responseCode = "409", description = "用户禁用商品下架库存不足或幂等冲突"),
        @ApiResponse(responseCode = "502", description = "下游响应无法确认"),
        @ApiResponse(responseCode = "503", description = "下游不可用或库存结果待确认"),
        @ApiResponse(responseCode = "500", description = "内部服务错误")
})
@Tag(name = "正式订单接口")
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderWriteController {
    private final OrderWriteService service;

    /**
     * @param service 正式订单业务服务
     */
    public OrderWriteController(OrderWriteService service) {
        this.service = service;
    }

    /**
     * @param dto 下单参数
     * @return 已保存且库存已确认的订单
     */
    @PostMapping
    @Operation(summary = "正式下单", description = "requestNo 为幂等键；库存调用未知时保留订单，可查询并重试")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    /**
     * @param orderNo 原订单号
     * @return 包含待确认或拒绝状态的订单
     */
    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单")
    public Result<OrderVO> detail(@PathVariable @Pattern(regexp = "[a-z0-9_-]{8,64}") String orderNo) {
        return Result.success(service.detail(orderNo));
    }

    /**
     * @param orderNo 原订单号
     * @return 沿用原商品、数量及价格的确认结果
     */
    @PostMapping("/{orderNo}/retry")
    @Operation(summary = "重试原订单", description = "相同订单号不会重复扣减；不创建新订单或重算价格")
    public Result<OrderVO> retry(@PathVariable @Pattern(regexp = "[a-z0-9_-]{8,64}") String orderNo) {
        return Result.success(service.retry(orderNo));
    }
}
