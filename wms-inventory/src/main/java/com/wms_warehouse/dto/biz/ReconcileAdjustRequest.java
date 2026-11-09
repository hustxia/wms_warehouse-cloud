package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 对账冲销请求（V1.1 F-08，内部端点 {@code POST /internal/inventory/reconcile-adjust}）。
 *
 * <p>由运维对账脚本调用，网关不路由。{@code targetOnHand} 一般取「该库存最近一条流水的
 * qty_after」——即把账面拉回最后一次合法变动后的值，而不是让脚本自己拍一个数。
 */
@Data
public class ReconcileAdjustRequest {

    @NotNull(message = "库存记录 ID 不能为空")
    private Long inventoryId;

    @NotNull(message = "目标在库量不能为空")
    private BigDecimal targetOnHand;

    /** 幂等键：同一差异重复冲销只写一条 ADJUST 流水 */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /** 流水关联单号，不传则用 requestId 生成 */
    private String refNo;

    private String remark;
}
