package com.wms_warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 发货单表保存 DTO（新增/修改）
 */
@Data
public class WmsShipmentSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "发货单号不能为空") @Size(max = 64, message = "发货单号长度不能超过64")
    private String shipmentNo;
    @NotNull(message = "出库单ID不能为空")
    private Long orderId;
    @Size(max = 64, message = "承运商长度不能超过64")
    private String carrier;
    @Size(max = 64, message = "运单号长度不能超过64")
    private String trackingNo;
    @NotNull(message = "包裹数不能为空")
    private Integer packageCount;
    private BigDecimal totalWeight;
    @NotBlank(message = "状态不能为空") @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    private LocalDateTime shippedAt;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
