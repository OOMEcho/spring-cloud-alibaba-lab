package cn.oomecho.cloud.order.controller;


import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.domain.dto.OrderPreviewDTO;
import cn.oomecho.cloud.order.domain.vo.OrderPreviewVO;
import cn.oomecho.cloud.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestController
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "请求参数错误或超过购买数量限制"),
        @ApiResponse(responseCode = "404", description = "用户不存在"),
        @ApiResponse(responseCode = "409", description = "用户已禁用"),
        @ApiResponse(responseCode = "429", description = "订单请求被限流"),
        @ApiResponse(responseCode = "502", description = "用户服务响应无效"),
        @ApiResponse(responseCode = "503", description = "用户服务不可用"),
        @ApiResponse(responseCode = "500", description = "内部服务错误")
})
@Tag(name = "订单接口")
public class OrderController {
    private final OrderService orderService;

    /**
     * @param orderService 订单业务层
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 预览订单并调用用户服务完成资格校验。
     *
     * @param dto 预览参数
     * @return 不落库的预览结果
     */
    @PostMapping("/preview")
    @Operation(summary = "订单预览", description = "校验用户、商品、数量和库存快照；不生成订单或扣减库存")
    public Result<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return Result.success(orderService.preview(dto));
    }
}
