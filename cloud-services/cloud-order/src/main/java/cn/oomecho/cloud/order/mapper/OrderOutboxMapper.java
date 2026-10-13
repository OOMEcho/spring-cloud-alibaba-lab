package cn.oomecho.cloud.order.mapper;

import cn.oomecho.cloud.order.domain.entity.OrderOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发件箱 SQL；数据库条件更新与令牌保护多个发布者的并发领取
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
public interface OrderOutboxMapper extends BaseMapper<OrderOutbox> {
    /**
     * @param orderNo 原订单号 @return 对应确认事件，当前最多一个
     */
    @Select("SELECT * FROM lab_order_outbox WHERE order_no=#{orderNo} ORDER BY created_at")
    List<OrderOutbox> byOrder(@Param("orderNo") String orderNo);

    /**
     * @param now UTC 当前时间 @param limit 本批上限 @param seataEnabled 是否允许处理全局事件 @return 可尝试领取的事件 ID
     */
    @Select("SELECT event_id FROM lab_order_outbox WHERE (global_xid IS NULL OR #{seataEnabled}=TRUE) AND ((status='PENDING' AND next_attempt_at<=#{now}) OR (status='PUBLISHING' AND lease_until<=#{now})) ORDER BY created_at LIMIT #{limit}")
    List<String> due(@Param("now") LocalDateTime now, @Param("limit") int limit, @Param("seataEnabled") boolean seataEnabled);

    /**
     * @param id 事件 ID @param now 当前时间 @param token 领取令牌 @param until 租约截止 @param max 次数上限 @return 成功领取行数
     */
    @Update("UPDATE lab_order_outbox SET status='PUBLISHING', attempts=attempts+1, lease_token=#{token}, lease_until=#{until} WHERE event_id=#{id} AND attempts<#{max} AND ((status='PENDING' AND next_attempt_at<=#{now}) OR (status='PUBLISHING' AND lease_until<=#{now}))")
    int claim(@Param("id") String id, @Param("now") LocalDateTime now, @Param("token") String token,
              @Param("until") LocalDateTime until, @Param("max") int max);

    /**
     * @param id 当前事件 ID @param now 当前时间 @param max 最大次数 @return 当前事件到达上限且过期时为 1
     */
    @Update("UPDATE lab_order_outbox SET status='FAILED', lease_token=NULL, lease_until=NULL, last_error='发布租约到期且重试次数已耗尽' WHERE event_id=#{id} AND status='PUBLISHING' AND lease_until<=#{now} AND attempts>=#{max}")
    int expireExhausted(@Param("id") String id, @Param("now") LocalDateTime now, @Param("max") int max);

    /**
     * @param id 事件 ID @param token 当前令牌 @param now 确认时间 @return 当前发布者完成行数
     */
    @Update("UPDATE lab_order_outbox SET status='SENT', sent_at=#{now}, lease_token=NULL, lease_until=NULL, last_error=NULL WHERE event_id=#{id} AND status='PUBLISHING' AND lease_token=#{token}")
    int sent(@Param("id") String id, @Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * @param id 事件 ID @param token 当前令牌 @param status PENDING 或 FAILED @param next 下次时间 @param error 公开原因 @return 修改行数
     */
    @Update("UPDATE lab_order_outbox SET status=#{status}, next_attempt_at=#{next}, last_error=#{error}, lease_token=NULL, lease_until=NULL WHERE event_id=#{id} AND status='PUBLISHING' AND lease_token=#{token}")
    int failed(@Param("id") String id, @Param("token") String token, @Param("status") String status,
               @Param("next") LocalDateTime next, @Param("error") String error);

    /**
     * @param id 事件 ID @param now 重试时间 @return 只允许待发布或已失败事件重试
     */
    @Update("UPDATE lab_order_outbox SET status='PENDING', attempts=0, next_attempt_at=#{now}, last_error=NULL WHERE event_id=#{id} AND status IN ('PENDING','FAILED')")
    int retry(@Param("id") String id, @Param("now") LocalDateTime now);
}
