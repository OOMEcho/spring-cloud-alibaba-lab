package cn.oomecho.cloud.notification.controller;

import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.notification.domain.vo.OrderNotificationVO;
import cn.oomecho.cloud.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知查询接口；异步事件未处理时返回明确的尚未创建状态
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@RestController
@RequestMapping("/notifications/orders")
@Tag(name = "订单站内通知")
@ApiResponses({@ApiResponse(responseCode = "200", description = "已创建的站内通知", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "订单号格式错误"),
        @ApiResponse(responseCode = "404", description = "通知尚未创建")})
public class NotificationController {
    private final NotificationService service;

    /**
     * @param service 通知查询服务
     */
    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /**
     * @param orderNo 原订单号 @return 异步保存的站内通知
     */
    @GetMapping("/{orderNo}")
    @Operation(summary = "按订单号查询站内通知", description = "订单已确认不代表通知已消费；未处理时返回 40408")
    public Result<OrderNotificationVO> detail(@PathVariable @Pattern(regexp = "[a-z0-9_-]{8,64}") String orderNo) {
        return Result.success(service.detail(orderNo));
    }
}
