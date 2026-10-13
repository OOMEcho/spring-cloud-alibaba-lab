package cn.oomecho.cloud.user.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户持久化实体；仅属于用户服务
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Data
@TableName("lab_user")
public class User {
    /**
     * 用户 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long id;
    /**
     * 用户名。
     */
    private String username;
    /**
     * 用户昵称。
     */
    private String nickname;
    /**
     * 是否允许下单。
     */
    private Boolean enabled;
}
