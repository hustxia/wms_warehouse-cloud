package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存锁定表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inventory_lock")
public class WmsInventoryLock extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 锁定流水号
     */
    private String lockNo;

    /**
     * 库存ID
     */
    private Long inventoryId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 库位ID
     */
    private Long locationId;

    /**
     * 锁定数量
     */
    private BigDecimal lockedQty;

    /**
     * 关联订单号
     */
    private String refOrderNo;

    /**
     * 锁定类型:PICK/TRANSFER/QC
     */
    private String lockType;

    /**
     * 状态:1锁定中 0已释放
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 释放时间
     */
    private LocalDateTime releasedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
