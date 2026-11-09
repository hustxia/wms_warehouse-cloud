package com.wms_warehouse.common.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单行分配请求（出库服务 → 库存服务）。
 *
 * <p>为什么要传 orderId/itemId 而不只是 skuId：分配要在库存侧落 {@code wms_allocation} 明细
 * （哪条库存被占了多少），这张表归库存中心所有，明细必须带得回单据与行的归属。
 */
@Data
public class AllocateItemRequest {

    private Long orderId;

    private String orderNo;

    private Long warehouseId;

    private Long ownerId;

    private Long itemId;

    private Long skuId;

    /** 还差多少没分配（order_qty - allocated_qty） */
    private BigDecimal needQty;
}
