package cn.oomecho.cloud.user.service;


import cn.oomecho.cloud.api.user.dto.UserResponse;
import cn.oomecho.cloud.user.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 用户 Entity 到跨服务响应的 MapStruct 映射
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserConvert {
    /**
     * @param user 数据库用户 @return 不包含持久化细节的用户响应
     */
    UserResponse toResponse(User user);
}
