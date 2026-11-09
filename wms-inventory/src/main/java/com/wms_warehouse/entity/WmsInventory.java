package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inventory")
public class WmsInventory extends BaseEntity {

    // <gen:begin key="entity-fields">
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
     * 生产日期
     */
    private LocalDate productionDate;

    /**
     * 到期日期
     */
    private LocalDate expiryDate;

    /**
     * 在库数量
     */
    private BigDecimal qtyOnHand;

    /**
     * 已分配数量
     */
    private BigDecimal qtyAllocated;

    /**
     * 锁定数量
     */
    private BigDecimal qtyLocked;

    /**
     * 可用数量
     */
    private BigDecimal qtyAvailable;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
