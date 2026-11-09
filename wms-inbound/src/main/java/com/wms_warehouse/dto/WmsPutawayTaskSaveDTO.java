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
 * 上架任务表保存 DTO（新增/修改）
 */
@Data
public class WmsPutawayTaskSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "上架任务号不能为空") @Size(max = 64, message = "上架任务号长度不能超过64")
    private String taskNo;
    @NotNull(message = "入库单ID不能为空")
    private Long orderId;
    @NotNull(message = "入库单明细ID不能为空")
    private Long itemId;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    private Long fromLocation;
    private Long toLocation;
    @NotBlank(message = "批次号不能为空") @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;
    @NotNull(message = "任务数量不能为空")
    private BigDecimal taskQty;
    @NotNull(message = "已完成数量不能为空")
    private BigDecimal doneQty;
    @NotBlank(message = "状态不能为空") @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    private Long assigneeId;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
