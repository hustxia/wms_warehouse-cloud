package com.wms_warehouse.dto.biz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 上架请求（POST /wms_inbound_order/{id}/putaway）。
 *
 * <p>上架必须指定目标库位：本版不做库位推荐算法，把「放哪」交给现场决定，
 * 系统负责保证「放了多少就加多少库存」。
 */
@Data
public class PutawayRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotEmpty(message = "上架明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        @NotNull(message = "目标库位不能为空")
        private Long locationId;

        @NotNull(message = "上架数量不能为空")
        @DecimalMin(value = "0.001", message = "上架数量必须大于 0")
        private BigDecimal putawayQty;
    }
}
