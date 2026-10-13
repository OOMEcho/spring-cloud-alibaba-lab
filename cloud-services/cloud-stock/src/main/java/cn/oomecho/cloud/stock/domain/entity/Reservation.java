package cn.oomecho.cloud.stock.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 库存幂等扣减记录；与库存数量在同一个本地事务中保存
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Data
@TableName("lab_stock_reservation")
public class Reservation {
    /**
     * 订单号，也是唯一幂等键。
     */
    @TableId(type = IdType.INPUT)
    private String orderNo;
    /**
     * 原始商品 ID，重复请求必须一致。
     */
    private Long productId;
    /**
     * 原始扣减数量，重复请求必须一致。
     */
    private Integer quantity;
    /**
     * PROCESSING 只在事务内使用；提交状态为 RESERVED 或 REJECTED。
     */
    private String status;
    /**
     * LOCAL 为原有本地事务流程，GLOBAL 为 Seata AT 流程，禁止同号跨模式使用。
     */
    private String transactionMode = "LOCAL";
    /**
     * 全局事务 ID，LOCAL 模式为空。
     */
    private String globalXid;
}
