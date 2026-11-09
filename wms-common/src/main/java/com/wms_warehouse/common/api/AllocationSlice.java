package com.wms_warehouse.common.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一次「占用/扣减」落在哪条库存上（库存服务 → 出库服务）。
 *
 * <p>分配与拣货都返回这个结构：分配时表示「占了哪条库存多少」，
 * 拣货时表示「从哪条库存拣走多少」。出库服务据此写拣货任务，不再自己猜批次与库位。
 */
@Data
public class AllocationSlice {

    private Long allocationId;

    private Long inventoryId;

    private Long locationId;

    private String batchNo;

    private BigDecimal qty;
}
