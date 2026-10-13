package cn.oomecho.cloud.product.mapper;


import cn.oomecho.cloud.product.domain.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * 商品数据访问层；更新时检查版本，不覆盖并发修改
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
public interface ProductMapper extends BaseMapper<Product> {
    /**
     * @param product 待更新商品及预期版本
     * @return 修改行数，0 表示版本冲突或不存在
     */
    @Update("UPDATE lab_product SET name=#{name}, price=#{price}, enabled=#{enabled}, version=version+1 WHERE id=#{id} AND version=#{version}")
    int updateVersioned(Product product);
}
