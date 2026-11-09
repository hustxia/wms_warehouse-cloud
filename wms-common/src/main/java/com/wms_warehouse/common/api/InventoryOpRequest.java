package com.wms_warehouse.common.api;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按库存主键的数量操作请求（内部接口）：减少在库量 / 分配 / 释放分配 / 冻结 / 解冻。
 *
 * <p>一个 DTO 承载五种动作而不是五个：字段集合完全一致，拆开只多五份重复定义；
 * 动作由调用路径决定，不会传错。
 */
@Data
public class InventoryOpRequest {

    private Long inventoryId;

    private BigDecimal qty;

    /** 流水类型：PICK / ADJUST / ALLOCATE / RELEASE_ALLOCATE / LOCK / UNLOCK */
    private String txnType;

    private String refNo;

    private String remark;

    /** 冻结类型：STOCKTAKE / QC / TRANSFER（仅 lock/unlock 使用） */
    private String lockType;
}
