package cn.oomecho.cloud.stock.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 库存持久化实体
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Data
@TableName("lab_stock")
public class Stock {
    /**
     * 商品 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long productId;
    /**
     * 可用库存，条件更新保障不为负数。
     */
    private Integer available;
}
