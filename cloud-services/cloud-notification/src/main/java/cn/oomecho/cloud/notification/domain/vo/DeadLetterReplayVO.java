package cn.oomecho.cloud.notification.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单条死信重新投递的 broker 接收结果；实际消费结果另查通知接口
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
public record DeadLetterReplayVO(
        @Schema(description = "队列非空且已重新投递时为 true") boolean replayed,
        @Schema(description = "原事件 ID；空队列时为 null") String eventId) {
}
