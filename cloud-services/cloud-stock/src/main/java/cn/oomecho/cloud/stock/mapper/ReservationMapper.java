package cn.oomecho.cloud.stock.mapper;


import cn.oomecho.cloud.api.stock.dto.ReservationRequest;
import cn.oomecho.cloud.stock.domain.entity.Reservation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 幂等库存记录访问层；唯一键串行化相同订单的首次处理
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface ReservationMapper extends BaseMapper<Reservation> {
    /**
     * @param request 已校验的参数
     * @return 1 为取得首次处理权，重复键由业务层捕获并读取原结果
     */
    @Insert("INSERT INTO lab_stock_reservation(order_no,product_id,quantity,status) VALUES(#{orderNo},#{productId},#{quantity},'PROCESSING')")
    int claim(ReservationRequest request);

    /**
     * @param id 原订单号 @return 带本地锁和全局锁检查的最新记录
     */
    @Select("SELECT * FROM lab_stock_reservation WHERE order_no=#{id} FOR UPDATE")
    Reservation locked(@Param("id") String id);
}
