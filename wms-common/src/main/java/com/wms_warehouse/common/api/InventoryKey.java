package com.wms_warehouse.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 库存业务键：仓库 + 货主 + SKU + 库位 + 批次，与 {@code wms_inventory} 唯一键 uk_inv 一一对应。
 *
 * <p>放在 wms-common 是因为它是**跨服务契约**：入库服务上架时要按它增加库存，
 * 库存服务按它定位记录。契约放在公共模块，避免两边各写一份字段顺序不一致的副本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryKey {

    private Long warehouseId;

    private Long ownerId;

    private Long skuId;

    private Long locationId;

    /** 非批次管理的 SKU 用空串，不用 null——唯一键里 null 不参与去重 */
    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;
}
