package cn.oomecho.cloud.order.controller;

import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.order.domain.entity.OrderOutbox;
import cn.oomecho.cloud.order.domain.vo.OrderEventVO;
import cn.oomecho.cloud.order.service.OutboxLocalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

/**
 * 学习项目的事件运维接口；查询与重新安排投递，不直接发送或覆盖事件内容
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@RestController
@RequestMapping("/orders/events")
@Tag(name = "订单事件投递")
@ApiResponses({@ApiResponse(responseCode = "200", description = "投递状态或已安排重试", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "事件 ID 格式错误"),
        @ApiResponse(responseCode = "404", description = "事件不存在"),
        @ApiResponse(responseCode = "409", description = "事件正在投递或 broker 已接收")})
public class OrderEventController {
    private final OutboxLocalService service;

    /**
     * @param service 发件箱业务服务
     */
    public OrderEventController(OutboxLocalService service) {
        this.service = service;
    }

    /**
     * @param orderNo 原订单号 @return 已产生的确认事件列表
     */
    @GetMapping("/by-order/{orderNo}")
    @Operation(summary = "按原订单号查询确认事件")
    public Result<java.util.List<OrderEventVO>> byOrder(@PathVariable @Pattern(regexp = "[a-z0-9_-]{8,64}") String orderNo) {
        return Result.success(service.byOrder(orderNo).stream().map(this::view).toList());
    }

    /**
     * @param id 事件 ID @return broker 投递状态，消费结果需到通知服务查询
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询事件投递状态")
    public Result<OrderEventVO> detail(@PathVariable @Pattern(regexp = "[a-f0-9-]{36}") String id) {
        return Result.success(view(service.require(id)));
    }

    /**
     * @param id 原事件 ID @return 已安排的投递，不代表立即完成
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "重新安排失败或待发布事件", description = "保留原事件 ID 和内容；SENT 不允许通过此接口重复发送")
    public Result<OrderEventVO> retry(@PathVariable @Pattern(regexp = "[a-f0-9-]{36}") String id) {
        return Result.success(view(service.retry(id)));
    }

    /**
     * @param row 发件箱行 @return 对外可见字段
     */
    private OrderEventVO view(OrderOutbox row) {
        return new OrderEventVO(row.getEventId(), row.getOrderNo(), row.getStatus(), row.getAttempts(), row.getNextAttemptAt(), row.getLastError());
    }
}
