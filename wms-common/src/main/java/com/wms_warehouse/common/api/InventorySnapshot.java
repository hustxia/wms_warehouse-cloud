package com.wms_warehouse.common.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存快照（内部接口返回）：出库服务做 FEFO 分配时需要的只读视图。
 *
 * <p>只暴露分配决策必需的字段，不把整行库存实体丢过网络——
 * 实体带逻辑删除、审计字段，跨服务传既无意义又会诱发「顺手改一下」。
 */
@Data
public class InventorySnapshot {

    private Long id;

    private Long locationId;

    private String batchNo;

    private LocalDate expiryDate;

    private BigDecimal qtyOnHand;

    private BigDecimal qtyAllocated;

    private BigDecimal qtyLocked;

    /** 可用量 = 在库 - 已分配 - 锁定（生成列口径，服务端算好再下发） */
    private BigDecimal available;
}
