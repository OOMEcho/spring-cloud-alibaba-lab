package cn.oomecho.cloud.order.mapper;


import cn.oomecho.cloud.order.domain.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 订单数据访问层；状态条件更新避免并发重试覆盖已确认结果
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * @param id 原订单号 @return 锁定读取，配合全局锁防止读取未完成的 AT 分支结果
     */
    @Select("SELECT * FROM lab_order WHERE order_no=#{id} FOR UPDATE")
    Order locked(@Param("id") String id);

    /**
     * @param orderNo 订单号
     * @param status  确定结果状态
     * @param error   公开原因
     * @return 修改行数
     */
    @Update("UPDATE lab_order SET status=#{status}, last_error=#{error}, updated_at=CURRENT_TIMESTAMP WHERE order_no=#{orderNo} AND status IN ('PENDING_STOCK','RECONCILE_REQUIRED')")
    int resolve(@Param("orderNo") String orderNo, @Param("status") String status, @Param("error") String error);

    /**
     * @param orderNo 订单号
     * @param error   公开原因
     * @return 修改行数；不能把已确认或已拒绝状态改回待确认
     */
    @Update("UPDATE lab_order SET status='RECONCILE_REQUIRED', last_error=#{error}, updated_at=CURRENT_TIMESTAMP WHERE order_no=#{orderNo} AND status='PENDING_STOCK'")
    int markUncertain(@Param("orderNo") String orderNo, @Param("error") String error);
}
