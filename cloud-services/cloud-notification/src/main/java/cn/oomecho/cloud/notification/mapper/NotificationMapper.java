package cn.oomecho.cloud.notification.mapper;

import cn.oomecho.cloud.notification.domain.vo.OrderNotificationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消费记录与站内通知的数据访问；去重行与业务写入使用同一本地事务
 *
 * @author xuesong.lei
 * @since 2026-10-13
 */
public interface NotificationMapper {
    /**
     * @param id 事件 ID @param hash 原 JSON 摘要 @param now 当前时间 @return 首次插入 1，已存在 0
     */
    @Insert("INSERT IGNORE INTO lab_message_inbox(event_id,payload_hash,processed_at) VALUES(#{id},#{hash},#{now})")
    int claim(@Param("id") String id, @Param("hash") String hash, @Param("now") LocalDateTime now);

    /**
     * @param id 原事件 ID @return 已处理 payload 摘要
     */
    @Select("SELECT payload_hash FROM lab_message_inbox WHERE event_id=#{id}")
    String hash(@Param("id") String id);

    /**
     * @param id 事件 ID @param orderNo 原订单号 @param userId 用户 @param content 站内内容 @param amount 原金额 @param now 创建时间 @return 写入行数
     */
    @Insert("INSERT INTO lab_order_notification(event_id,order_no,user_id,content,total_amount,created_at) VALUES(#{id},#{orderNo},#{userId},#{content},#{amount},#{now})")
    int insert(@Param("id") String id, @Param("orderNo") String orderNo, @Param("userId") Long userId,
               @Param("content") String content, @Param("amount") BigDecimal amount, @Param("now") LocalDateTime now);

    /**
     * @param orderNo 原订单号 @return 站内通知，没有消费完成时为 null
     */
    @Select("SELECT event_id,order_no,user_id,content,total_amount,created_at FROM lab_order_notification WHERE order_no=#{orderNo}")
    OrderNotificationVO find(@Param("orderNo") String orderNo);
}
