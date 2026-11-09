package com.wms_warehouse.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仓库主表保存 DTO（新增/修改）
 */
@Data
public class WmsWarehouseSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "仓库编码不能为空") @Size(max = 32, message = "仓库编码长度不能超过32")
    private String warehouseCode;
    @NotBlank(message = "仓库名称不能为空") @Size(max = 128, message = "仓库名称长度不能超过128")
    private String warehouseName;
    @Size(max = 255, message = "仓库地址长度不能超过255")
    private String address;
    @Size(max = 64, message = "联系人长度不能超过64")
    private String contactPerson;
    @Size(max = 32, message = "联系电话长度不能超过32")
    private String contactPhone;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    @NotNull(message = "更新时间不能为空")
    private LocalDateTime updatedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
