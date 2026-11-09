package com.wms_warehouse.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 库存表保存 DTO（新增/修改）
 */
@Data
public class WmsInventorySaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @NotNull(message = "货主ID不能为空")
    private Long ownerId;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    @NotNull(message = "库位ID不能为空")
    private Long locationId;
    @NotBlank(message = "批次号不能为空") @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    // V1.1 F-03：数量/版本由 InventoryCoreService 唯一入口写入，qty_available 是生成列。
    // 全部去掉 @NotNull：通用 CRUD 剔除这些字段后要靠 DB 默认值（qty_* DEFAULT 0、
    // version DEFAULT 0、updated_at ON UPDATE CURRENT_TIMESTAMP）落地。
    // 若保留 @NotNull，前端一剔除字段就会被 @Valid 拦死，只读规则反而让页面不可用。
    private BigDecimal qtyOnHand;
    private BigDecimal qtyAllocated;
    private BigDecimal qtyLocked;
    private BigDecimal qtyAvailable;
    private Integer version;
    private LocalDateTime updatedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
