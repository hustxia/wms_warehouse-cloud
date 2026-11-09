package com.wms_warehouse.common.api;

import lombok.Data;

/**
 * FEFO 候选库存查询条件（内部接口）。
 */
@Data
public class FefoQuery {

    private Long warehouseId;

    private Long ownerId;

    private Long skuId;
}
