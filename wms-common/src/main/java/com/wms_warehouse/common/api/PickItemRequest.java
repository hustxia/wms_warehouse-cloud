package com.wms_warehouse.common.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拣货请求（出库服务 → 库存服务）：本行要拣多少。
 */
@Data
public class PickItemRequest {

    private Long orderId;

    private String orderNo;

    private Long itemId;

    private BigDecimal qty;
}
