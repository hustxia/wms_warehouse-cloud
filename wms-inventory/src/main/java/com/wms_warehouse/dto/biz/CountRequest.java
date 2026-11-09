package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 录实盘数量（POST /wms_stocktake_item/{id}/count）。
 *
 * <p>允许填 0（实物确实一件不剩），不允许负数。差异量由系统算，前端不传。
 */
@Data
public class CountRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotNull(message = "实盘数量不能为空")
    @DecimalMin(value = "0", message = "实盘数量不能为负数")
    private BigDecimal countedQty;
}
