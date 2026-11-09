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
 * 库存事务流水表保存 DTO（新增/修改）
 */
@Data
public class WmsInventoryTxnSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "事务流水号不能为空") @Size(max = 64, message = "事务流水号长度不能超过64")
    private String txnNo;
    @NotBlank(message = "类型不能为空") @Size(max = 32, message = "类型长度不能超过32")
    private String txnType;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @NotNull(message = "货主ID不能为空")
    private Long ownerId;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    private Long locationId;
    @NotBlank(message = "批次号不能为空") @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;
    @NotNull(message = "变动数量不能为空")
    private BigDecimal qtyChange;
    private BigDecimal qtyBefore;
    private BigDecimal qtyAfter;
    @Size(max = 64, message = "关联单据号长度不能超过64")
    private String refOrderNo;
    private Long operatorId;
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
    @NotNull(message = "创建时间不能为空")
    private LocalDateTime createdAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
