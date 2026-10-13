package cn.oomecho.cloud.user.controller;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.common.core.result.Result;
import cn.oomecho.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "用户不存在"),
        @ApiResponse(responseCode = "500", description = "内部服务错误")
})
@Tag(name = "用户接口")
public class UserController {
    private final UserService userService;

    /**
     * @param userService 用户业务层
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 按 ID 查询用户，ID 必须为正整数。
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "用户详情", description = "实验数据：1 为正常用户，2 为禁用用户")
    public Result<UserResponse> detail(@PathVariable @Positive Long id) {
        return Result.success(userService.detail(id));
    }
}
