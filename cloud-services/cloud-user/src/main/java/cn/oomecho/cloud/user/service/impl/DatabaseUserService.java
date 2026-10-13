package cn.oomecho.cloud.user.service.impl;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.common.core.exception.BusinessException;
import cn.oomecho.cloud.user.mapper.UserMapper;
import cn.oomecho.cloud.user.service.UserConvert;
import cn.oomecho.cloud.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户数据库业务实现；查询 MySQL 并转换为用户响应契约
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Service
public class DatabaseUserService implements UserService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseUserService.class);
    private final UserMapper mapper;
    private final UserConvert convert;

    /**
     * @param mapper 用户 Mapper @param convert 类型映射器
     */
    public DatabaseUserService(UserMapper mapper, UserConvert convert) {
        this.mapper = mapper;
        this.convert = convert;
    }

    /**
     * @param id 用户 ID @return 数据库用户信息
     */
    @Override
    public UserResponse detail(Long id) {
        log.info("数据库查询用户，userId={}", id);
        var user = mapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, 40401, "用户不存在");
        }
        return convert.toResponse(user);
    }
}
