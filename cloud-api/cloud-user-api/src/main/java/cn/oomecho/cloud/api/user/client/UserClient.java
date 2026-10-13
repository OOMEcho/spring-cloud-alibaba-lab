package cn.oomecho.cloud.api.user.client;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务远程调用契约；不依赖用户服务实现
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@FeignClient(name = "cloud-user", contextId = "userClient", url = "${clients.user.url:}")
public interface UserClient {
    /**
     * 按用户 ID 查询用户；正常模式通过 Nacos 服务名发现实例。
     *
     * @param id 用户 ID
     * @return 用户信息的统一响应
     */
    @GetMapping("/users/{id}")
    Result<UserResponse> detail(@PathVariable("id") Long id);
}
