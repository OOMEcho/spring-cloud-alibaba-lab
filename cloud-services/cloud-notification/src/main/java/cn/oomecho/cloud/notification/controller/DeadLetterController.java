package cn.oomecho.cloud.notification.controller;

import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.notification.domain.vo.DeadLetterReplayVO;
import cn.oomecho.cloud.notification.service.DeadLetterReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习环境死信运维入口；先修复消费故障，再单条重放，不清空死信队列
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
@RestController
@RequestMapping("/notifications/dead-letters")
@ConditionalOnProperty(name = "lab.messaging.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "死信运维")
@ApiResponses({@ApiResponse(responseCode = "200", description = "单条重放结果；不代表消费完成", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "503", description = "重放接收结果未知，消息仍可处理")})
public class DeadLetterController {
    private final DeadLetterReplayService service;

    /**
     * @param service 当前项目队列的重放服务
     */
    public DeadLetterController(DeadLetterReplayService service) {
        this.service = service;
    }

    /**
     * @return broker 接收状态；失败原因修复前重复调用可能再次生成死信
     */
    @PostMapping("/replay-one")
    @Operation(summary = "修复故障后重放一条死信", description = "保持原事件 ID 和内容；重发获 broker 确认后才 ACK 原死信；不自动无限重放")
    public Result<DeadLetterReplayVO> replayOne() {
        return Result.success(service.replayOne());
    }
}
