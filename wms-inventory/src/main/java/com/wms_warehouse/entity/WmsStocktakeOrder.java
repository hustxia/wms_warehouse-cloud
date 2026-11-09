package com.wms_warehouse.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 盘点单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_stocktake_order")
public class WmsStocktakeOrder extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 盘点单号
     */
    private String stocktakeNo;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 盘点库区(可空=全仓)
     */
    private Long zoneId;

    /**
     * 类型:全盘/抽盘/循环盘点
     */
    private String stocktakeType;

    /**
     * 状态:CREATED/COUNTING/DIFF_PENDING/CONFIRMED/CANCELLED
     */
    private String status;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
