package com.wms_warehouse.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 货主表保存 DTO（新增/修改）
 */
@Data
public class WmsOwnerSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "货主编码不能为空") @Size(max = 32, message = "货主编码长度不能超过32")
    private String ownerCode;
    @NotBlank(message = "货主名称不能为空") @Size(max = 128, message = "货主名称长度不能超过128")
    private String ownerName;
    @Size(max = 64, message = "联系人长度不能超过64")
    private String contact;
    @Size(max = 32, message = "联系电话长度不能超过32")
    private String phone;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
