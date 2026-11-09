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
 * 库存锁定表保存 DTO（新增/修改）
 */
@Data
public class WmsInventoryLockSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "锁定流水号不能为空") @Size(max = 64, message = "锁定流水号长度不能超过64")
    private String lockNo;
    @NotNull(message = "库存ID不能为空")
    private Long inventoryId;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    @NotNull(message = "库位ID不能为空")
    private Long locationId;
    @NotNull(message = "锁定数量不能为空")
    private BigDecimal lockedQty;
    @Size(max = 64, message = "关联订单号长度不能超过64")
    private String refOrderNo;
    @Size(max = 32, message = "锁定类型长度不能超过32")
    private String lockType;
    @NotNull(message = "状态不能为空")
    private Integer status;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
