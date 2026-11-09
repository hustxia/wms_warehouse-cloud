package com.wms_warehouse.common.api;

import lombok.Data;

/**
 * SKU 信息（主数据服务内部接口返回）：入库上架时判断是否批次管理、按保质期推算效期。
 */
@Data
public class SkuInfo {

    private Long id;

    private String skuCode;

    /** 1 批次管理 0 不管批次 */
    private Integer isBatch;

    /** 保质期天数，可空；为空时上架不推算效期 */
    private Integer shelfLife;
}
