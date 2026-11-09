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
 * 移库单表保存 DTO（新增/修改）
 */
@Data
public class WmsTransferOrderSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "移库单号不能为空") @Size(max = 64, message = "移库单号长度不能超过64")
    private String transferNo;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @NotNull(message = "来源库位ID不能为空")
    private Long fromLocation;
    @NotNull(message = "目标库位ID不能为空")
    private Long toLocation;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    @NotBlank(message = "批次号不能为空") @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;
    @NotNull(message = "移库数量不能为空")
    private BigDecimal transferQty;
    @NotBlank(message = "状态不能为空") @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    private Long operatorId;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
