package com.wms_warehouse.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 出库单主表保存 DTO（新增/修改）
 */
@Data
public class WmsOutboundOrderSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "出库单号不能为空") @Size(max = 64, message = "出库单号长度不能超过64")
    private String orderNo;
    @NotBlank(message = "类型不能为空") @Size(max = 32, message = "类型长度不能超过32")
    private String orderType;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @NotNull(message = "货主ID不能为空")
    private Long ownerId;
    private Long customerId;
    @Size(max = 64, message = "外部单号长度不能超过64")
    private String refNo;
    // V1.1 F-03：status / carrier / trackingNo 只能由业务端点写，通用 CRUD 不接受；
    // 去掉 status 的 @NotBlank，前端剔除只读字段后才能通过 @Valid。
    @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    @NotNull(message = "优先级不能为空")
    private Integer priority;
    @Size(max = 64, message = "承运商长度不能超过64")
    private String carrier;
    @Size(max = 64, message = "运单号长度不能超过64")
    private String trackingNo;
    // 审计时间由 DB / AuditMetaObjectHandler 填充（NOT_NULL 策略下 null 不进 SQL）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
