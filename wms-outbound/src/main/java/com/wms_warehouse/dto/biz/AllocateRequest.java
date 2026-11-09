package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分配请求（POST /wms_outbound_order/{id}/allocate）。
 *
 * <p>没有数量参数：分配永远按单据需求量做，能分多少分多少。
 * 允许「部分分配」是仓储现实——缺货时不能把整张单卡死，剩下的等补货。
 */
@Data
public class AllocateRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;
}
