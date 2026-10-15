package cn.oomecho.cloud.order.controller;

import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.domain.vo.ProductDisplayVO;
import cn.oomecho.cloud.order.integration.ProductDisplayGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单侧可选商品展示接口，用于对照展示降级与下单关键校验的故障策略。
 *
 * @author xuesong.lei
 * @since 2026-10-14
 */
@Validated
@RestController
@RequestMapping("/orders/products")
@Tag(name = "订单商品展示与降级")
public class ProductDisplayController {
    /** 展示调用适配层。 */
    private final ProductDisplayGateway gateway;

    /** @param gateway 商品展示调用适配层 */
    public ProductDisplayController(ProductDisplayGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * @param id 商品 ID
     * @return 展示结果，故障时明确标记 degraded，商品字段为空
     */
    @GetMapping("/{id}/display")
    @Operation(summary = "商品展示参考信息（允许降级）", description = "故障时 HTTP 200 且 degraded=true；不确认商品存在、价格或购买资格。正式下单独立校验用户、数据库报价和库存。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "正常或显式降级结果", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "商品 ID 非法"),
            @ApiResponse(responseCode = "404", description = "商品不存在，不采用降级"),
            @ApiResponse(responseCode = "502", description = "商品服务返回无效数据，不采用降级")})
    public Result<ProductDisplayVO> detail(@PathVariable @Positive Long id) {
        return Result.success(gateway.detail(id));
    }
}
