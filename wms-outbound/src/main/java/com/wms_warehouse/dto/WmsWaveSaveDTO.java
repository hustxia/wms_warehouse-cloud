package com.wms_warehouse.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 波次表保存 DTO（新增/修改）
 */
@Data
public class WmsWaveSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "波次号不能为空") @Size(max = 64, message = "波次号长度不能超过64")
    private String waveNo;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @Size(max = 32, message = "类型长度不能超过32")
    private String waveType;
    @NotNull(message = "订单数量不能为空")
    private Integer orderCount;
    @NotBlank(message = "状态不能为空") @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
