package com.wms_warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存分配明细表保存 DTO（新增/修改）
 */
@Data
public class WmsAllocationSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotNull(message = "出库单ID不能为空")
    private Long orderId;
    @NotNull(message = "出库单明细ID不能为空")
    private Long itemId;
    @NotNull(message = "库存ID不能为空")
    private Long inventoryId;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    @NotNull(message = "库位ID不能为空")
    private Long locationId;
    @NotBlank(message = "批次号不能为空") @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;
    @NotNull(message = "分配数量不能为空")
    private BigDecimal allocatedQty;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
