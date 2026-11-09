package com.wms_warehouse.common.api;

import lombok.Data;

/**
 * 库位信息（主数据服务内部接口返回）：入库上架校验「库位存在且属于本仓」时用。
 */
@Data
public class LocationInfo {

    private Long id;

    private String locationCode;

    private Long warehouseId;

    private Long zoneId;
}
