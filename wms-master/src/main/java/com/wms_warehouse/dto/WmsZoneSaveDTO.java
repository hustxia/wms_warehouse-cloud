package com.wms_warehouse.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库区表保存 DTO（新增/修改）
 */
@Data
public class WmsZoneSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotNull(message = "所属仓库ID不能为空")
    private Long warehouseId;
    @NotBlank(message = "库区编码不能为空") @Size(max = 32, message = "库区编码长度不能超过32")
    private String zoneCode;
    @NotBlank(message = "库区名称不能为空") @Size(max = 128, message = "库区名称长度不能超过128")
    private String zoneName;
    @Size(max = 32, message = "类型长度不能超过32")
    private String zoneType;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
