package com.wms_warehouse.common.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 增加在库量请求（内部接口）。按业务键定位，不存在则由库存服务建记录。
 */
@Data
public class InventoryIncreaseRequest {

    private InventoryKey key;

    private BigDecimal qty;

    /** 流水类型：PUTAWAY / ADJUST / RECEIPT ... */
    private String txnType;

    /** 关联单据号，写进流水 ref_order_no */
    private String refNo;

    private String remark;
}
