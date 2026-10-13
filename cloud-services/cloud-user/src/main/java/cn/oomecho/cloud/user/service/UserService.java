package cn.oomecho.cloud.user.service;


import cn.oomecho.cloud.api.user.dto.UserResponse;

/**
 * 用户业务层
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface UserService {
    /**
     * 查询用户，不存在时抛出业务异常。
     *
     * @param id 用户 ID
     * @return 用户查询契约
     */
    UserResponse detail(Long id);
}
