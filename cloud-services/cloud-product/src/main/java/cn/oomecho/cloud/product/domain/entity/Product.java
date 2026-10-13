package cn.oomecho.cloud.product.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品持久化实体；不跨服务共享
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Data
@TableName("lab_product")
public class Product {
    /**
     * 商品 ID。
     */
    @TableId(type = IdType.INPUT)
    private Long id;
    /**
     * 商品名称。
     */
    private String name;
    /**
     * 数据库单价，单位元。
     */
    private BigDecimal price;
    /**
     * 是否上架。
     */
    private Boolean enabled;
    /**
     * 并发修改版本号。
     */
    private Integer version;
}
