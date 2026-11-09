package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发货单表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_shipment")
public class WmsShipment extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 发货单号
     */
    private String shipmentNo;

    /**
     * 出库单ID
     */
    private Long orderId;

    /**
     * 承运商
     */
    private String carrier;

    /**
     * 运单号
     */
    private String trackingNo;

    /**
     * 包裹数
     */
    private Integer packageCount;

    /**
     * 总重量(kg)
     */
    private BigDecimal totalWeight;

    /**
     * 状态:PENDING/SHIPPED/DELIVERED
     */
    private String status;

    /**
     * 发货时间
     */
    private LocalDateTime shippedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
