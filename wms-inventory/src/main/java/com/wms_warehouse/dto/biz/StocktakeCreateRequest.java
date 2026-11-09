package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建盘点单请求（POST /wms_stocktake_order）。
 *
 * <p>范围三选一：指定库位 &gt; 指定库区 &gt; 全仓。创建即快照，快照后库存再变不影响本次盘点。
 */
@Data
public class StocktakeCreateRequest {

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    private Long zoneId;

    private Long locationId;

    /** 全盘 / 抽盘 / 循环盘点，仅作标记，不影响逻辑 */
    private String stocktakeType;

    /** 可选：带上幂等键时，重复提交不会创建第二张盘点单 */
    private String requestId;
}
