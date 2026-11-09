package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 盘点明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stocktake_item")
public class WmsStocktakeItem extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 盘点单ID
     */
    private Long stocktakeId;

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
     * 批次号
     */
    private String batchNo;

    /**
     * 系统账面数量
     */
    private BigDecimal systemQty;

    /**
     * 实盘数量
     */
    private BigDecimal countedQty;

    /**
     * 差异数量
     */
    private BigDecimal diffQty;

    /**
     * 状态:PENDING/COUNTED/CONFIRMED
     */
    private String status;

    /**
     * 盘点人
     */
    private Long countedBy;

    /**
     * 盘点时间
     */
    private LocalDateTime countedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
