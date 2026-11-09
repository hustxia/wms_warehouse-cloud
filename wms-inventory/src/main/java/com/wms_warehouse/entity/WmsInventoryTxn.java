package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存事务流水表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inventory_txn")
public class WmsInventoryTxn extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 事务流水号
     */
    private String txnNo;

    /**
     * 类型:RECEIPT/PUTAWAY/PICK/SHIP/ADJUST/TRANSFER/MOVE
     */
    private String txnType;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 货主ID
     */
    private Long ownerId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 库位ID
     */
    private Long locationId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 变动数量(正负)
     */
    private BigDecimal qtyChange;

    /**
     * 变动前数量
     */
    private BigDecimal qtyBefore;

    /**
     * 变动后数量
     */
    private BigDecimal qtyAfter;

    /**
     * 关联单据号
     */
    private String refOrderNo;

    /**
     * 操作人
     */
    private Long operatorId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
