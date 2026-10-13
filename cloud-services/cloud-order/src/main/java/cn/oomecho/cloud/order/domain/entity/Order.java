package cn.oomecho.cloud.order.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单持久化实体；先保存意图，再确认幂等库存结果
 *
 * @author xuesong.lei
 * @since 2026-10-12
 */
@Data
@TableName("lab_order")
public class Order {
    /**
     * 客户端请求号，作为订单唯一幂等键。
     */
    @TableId(type = IdType.INPUT)
    private String orderNo;
    /**
     * 用户 ID 快照。
     */
    private Long userId;
    /**
     * 商品 ID。
     */
    private Long productId;
    /**
     * 下单时商品名称快照。
     */
    private String productName;
    /**
     * 商品服务数据库提供的单价快照。
     */
    private BigDecimal unitPrice;
    /**
     * 购买数量。
     */
    private Integer quantity;
    /**
     * 服务端计算的订单金额，单位元。
     */
    private BigDecimal totalAmount;
    /**
     * PENDING_STOCK、CONFIRMED、REJECTED、RECONCILE_REQUIRED。
     */
    private String status;
    /**
     * 待确认或拒绝的公开原因，不保存内部异常堆栈。
     */
    private String lastError;
    /**
     * 数据库创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 数据库状态变更时间。
     */
    private LocalDateTime updatedAt;
    /**
     * LOCAL 为原有本地事务流程，GLOBAL 为 Seata AT 流程，禁止同号跨模式使用。
     */
    private String transactionMode = "LOCAL";
    /**
     * 全局事务 ID，LOCAL 模式为空。
     */
    private String globalXid;
}
