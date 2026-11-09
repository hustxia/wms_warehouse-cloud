package com.wms_warehouse.dto.biz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收货登记请求（POST /wms_inbound_order/{id}/receive）。
 *
 * <p>支持逐行填实收数量，也允许覆盖批次与效期——供应商换批次时以实收为准，
 * 但只在该行原批次为空时生效，避免把计划批次改成别的。
 */
@Data
public class ReceiveRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotEmpty(message = "收货明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        @NotNull(message = "收货数量不能为空")
        @DecimalMin(value = "0.001", message = "收货数量必须大于 0")
        private BigDecimal receivedQty;

        private String batchNo;

        private LocalDate productionDate;

        private LocalDate expiryDate;
    }
}
