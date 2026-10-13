package cn.oomecho.cloud.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 用户查询契约；仅传递 id、用户名、昵称和启用状态，不传递密码或持久化实体
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Schema(description = "用户查询响应")
public record UserResponse(
        @Schema(description = "用户 ID", example = "1") Long id,
        @Schema(description = "用户名", example = "demo") String username,
        @Schema(description = "用户昵称", example = "演示用户") String nickname,
        @Schema(description = "是否启用", example = "true") boolean enabled) {
}
