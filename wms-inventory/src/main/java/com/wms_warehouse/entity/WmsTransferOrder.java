package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 移库单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_transfer_order")
public class WmsTransferOrder extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 移库单号
     */
    private String transferNo;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 来源库位ID
     */
    private Long fromLocation;

    /**
     * 目标库位ID
     */
    private Long toLocation;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 移库数量
     */
    private BigDecimal transferQty;

    /**
     * 状态:CREATED/DOING/DONE/CANCELLED
     */
    private String status;

    /**
     * 操作人
     */
    private Long operatorId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    private LocalDateTime finishedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
