package com.wms_warehouse.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 入库单明细表保存 DTO（新增/修改）
 */
@Data
public class WmsInboundOrderItemSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotNull(message = "入库单ID不能为空")
    private Long orderId;
    @NotNull(message = "行号不能为空")
    private Integer lineNo;
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;
    @NotBlank(message = "批次号不能为空") @Size(max = 64, message = "批次号长度不能超过64")
    private String batchNo;
    @NotNull(message = "预期数量不能为空")
    private BigDecimal expectedQty;
    @NotNull(message = "实收数量不能为空")
    private BigDecimal receivedQty;
    @NotNull(message = "已上架数量不能为空")
    private BigDecimal putawayQty;
    private Long locationId;
    @NotBlank(message = "状态不能为空") @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
