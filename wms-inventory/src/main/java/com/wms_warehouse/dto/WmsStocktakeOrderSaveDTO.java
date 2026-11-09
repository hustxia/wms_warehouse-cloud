package com.wms_warehouse.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 盘点单表保存 DTO（新增/修改）
 */
@Data
public class WmsStocktakeOrderSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "盘点单号不能为空") @Size(max = 64, message = "盘点单号长度不能超过64")
    private String stocktakeNo;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    private Long zoneId;
    @Size(max = 32, message = "类型长度不能超过32")
    private String stocktakeType;
    // V1.1 F-03：状态与确认时间由盘点业务端点写入，通用 CRUD 不接受也不强制必填。
    @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    private Long createdBy;
    // 审计时间由 DB 默认值填充（NOT_NULL 策略下 null 不进 SQL）
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
