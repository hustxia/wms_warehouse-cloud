package com.wms_warehouse.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入库单主表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inbound_order")
public class WmsInboundOrder extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 入库单号
     */
    private String orderNo;

    /**
     * 类型:PURCHASE/RETURN/TRANSFER_IN
     */
    private String orderType;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 货主ID
     */
    private Long ownerId;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 外部单号(ERP采购单号)
     */
    private String refNo;

    /**
     * 状态:CREATED/IN_TRANSIT/RECEIVING/RECEIVED/PUTAWAY_DONE/CANCELLED
     */
    private String status;

    /**
     * 预计到货日期
     */
    private LocalDate expectedDate;

    /**
     * 操作人
     */
    private Long operatorId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
