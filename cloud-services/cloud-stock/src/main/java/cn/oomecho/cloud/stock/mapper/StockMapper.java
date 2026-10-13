package cn.oomecho.cloud.stock.mapper;


import cn.oomecho.cloud.stock.domain.entity.Stock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存数据访问层；单条条件更新避免并发超卖
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface StockMapper extends BaseMapper<Stock> {
    /**
     * @param id       商品 ID
     * @param quantity 扣减数量
     * @return 1 为扣减成功，0 为库存不足或不存在
     */
    @Update("UPDATE lab_stock SET available=available-#{quantity} WHERE product_id=#{id} AND available>=#{quantity}")
    int decrease(@Param("id") Long id, @Param("quantity") Integer quantity);
}
