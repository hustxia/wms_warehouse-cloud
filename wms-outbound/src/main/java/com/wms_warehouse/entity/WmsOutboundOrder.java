package com.wms_warehouse.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出库单主表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_outbound_order")
public class WmsOutboundOrder extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 出库单号
     */
    private String orderNo;

    /**
     * 类型:SALES/RETURN_TO_SUPPLIER/TRANSFER_OUT
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
     * 客户ID
     */
    private Long customerId;

    /**
     * 外部单号(ERP销售单号)
     */
    private String refNo;

    /**
     * 状态:CREATED/ALLOCATED/PICKING/PICKED/SHIPPED/CANCELLED
     */
    private String status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 承运商
     */
    private String carrier;

    /**
     * 运单号
     */
    private String trackingNo;

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
