package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 拣货确认请求（POST /wms_outbound_order/{id}/pick）。
 *
 * <p>只填「实拣数量」，不填库位与批次：捡哪个批次由分配结果决定（FEFO），
 * 让前端再选一次批次等于给了绕过 FEFO 的口子。
 */
@Data
public class PickRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotEmpty(message = "拣货明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        @NotNull(message = "拣货数量不能为空")
        @DecimalMin(value = "0.001", message = "拣货数量必须大于 0")
        private BigDecimal pickedQty;
    }
}
