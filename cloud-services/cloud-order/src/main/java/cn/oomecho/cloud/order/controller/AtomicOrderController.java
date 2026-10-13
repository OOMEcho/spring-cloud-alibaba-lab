package cn.oomecho.cloud.order.controller;

import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.domain.dto.OrderCreateDTO;
import cn.oomecho.cloud.order.domain.vo.OrderVO;
import cn.oomecho.cloud.order.service.AtomicOrderFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seata 全局事务下单入口；与普通下单并存便于比较，禁止同号跨模式使用
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@RestController
@ConditionalOnProperty(name = "seata.enabled", havingValue = "true")
@Tag(name = "Seata 全局事务订单")
@ApiResponses({@ApiResponse(responseCode = "200", description = "全局事务提交后的已确认订单", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "请求格式或数量限制错误"),
        @ApiResponse(responseCode = "404", description = "用户或商品不存在"),
        @ApiResponse(responseCode = "409", description = "库存不足、用户禁用、模式或幂等冲突"),
        @ApiResponse(responseCode = "503", description = "下游或全局事务结果暂不能确认")})
public class AtomicOrderController {
    private final AtomicOrderFacade service;

    /**
     * @param service 全局代理外编排
     */
    public AtomicOrderController(AtomicOrderFacade service) {
        this.service = service;
    }

    /**
     * @param dto 原请求号与购买参数 @return 已全局提交的订单，消息消费仍是异步
     */
    @PostMapping("/orders/atomic")
    @Operation(summary = "Seata AT 全局下单", description = "库存分支或订单保存失败将触发全局回滚；事件等全局提交后投递；未知响应需保留原号查询")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(service.create(dto));
    }
}
