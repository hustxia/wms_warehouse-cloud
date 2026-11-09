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
 * 商品SKU表保存 DTO（新增/修改）
 */
@Data
public class WmsSkuSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "SKU编码不能为空") @Size(max = 64, message = "SKU编码长度不能超过64")
    private String skuCode;
    @NotBlank(message = "商品名称不能为空") @Size(max = 255, message = "商品名称长度不能超过255")
    private String skuName;
    @Size(max = 64, message = "条码长度不能超过64")
    private String barcode;
    private Long categoryId;
    @Size(max = 128, message = "规格长度不能超过128")
    private String spec;
    @Size(max = 16, message = "计量单位长度不能超过16")
    private String unit;
    private BigDecimal weight;
    private BigDecimal volume;
    private Integer shelfLife;
    @NotNull(message = "是否批次管理不能为空")
    private Integer isBatch;
    @NotNull(message = "是否序列号管理不能为空")
    private Integer isSerial;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
