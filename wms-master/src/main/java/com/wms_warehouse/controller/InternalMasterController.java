package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms_warehouse.common.api.LocationInfo;
import com.wms_warehouse.common.api.SkuInfo;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import com.wms_warehouse.entity.WmsLocation;
import com.wms_warehouse.entity.WmsSku;
import com.wms_warehouse.service.WmsLocationService;
import com.wms_warehouse.service.WmsSkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 主数据内部接口（服务间调用，网关不路由）。
 *
 * <p>入库服务需要「库位是否存在、属于哪个仓」「SKU 是否批次管理、保质期多少」，
 * 这些数据归 wms-master 所有。让入库服务直连 wms_location/wms_sku 表能少写两个接口，
 * 但那样主数据的口径就散落在各服务里了——表是共享的，规则不共享。
 */
@Tag(name = "主数据内部接口", description = "仅供服务间调用，网关不路由")
@RestController
@RequestMapping("/internal/master")
public class InternalMasterController {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    @Value("${app.internal.token}")
    private String internalToken;

    @Autowired
    private WmsLocationService locationService;

    @Autowired
    private WmsSkuService skuService;

    @Operation(summary = "按 ID 查库位")
    @GetMapping("/location/{id}")
    public Result<LocationInfo> location(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                         @PathVariable("id") Long id) {
        checkToken(token);
        WmsLocation location = locationService.getById(id);
        if (location == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "库位不存在：" + id);
        }
        LocationInfo info = new LocationInfo();
        info.setId(location.getId());
        info.setLocationCode(location.getLocationCode());
        info.setWarehouseId(location.getWarehouseId());
        info.setZoneId(location.getZoneId());
        return Result.ok(info);
    }

    @Operation(summary = "按 ID 查 SKU")
    @GetMapping("/sku/{id}")
    public Result<SkuInfo> sku(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                               @PathVariable("id") Long id) {
        checkToken(token);
        WmsSku sku = skuService.getById(id);
        if (sku == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "SKU 不存在：" + id);
        }
        SkuInfo info = new SkuInfo();
        info.setId(sku.getId());
        info.setSkuCode(sku.getSkuCode());
        info.setIsBatch(sku.getIsBatch());
        info.setShelfLife(sku.getShelfLife());
        return Result.ok(info);
    }

    @Operation(summary = "按库区查库位 ID 列表（盘点按库区取快照范围）")
    @GetMapping("/zone/{zoneId}/locations")
    public Result<List<Long>> zoneLocations(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                            @PathVariable("zoneId") Long zoneId) {
        checkToken(token);
        List<Long> ids = locationService.list(new LambdaQueryWrapper<WmsLocation>()
                        .eq(WmsLocation::getZoneId, zoneId))
                .stream().map(WmsLocation::getId).toList();
        return Result.ok(ids);
    }

    private void checkToken(String token) {
        if (!StringUtils.hasText(token) || !token.equals(internalToken)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "内部调用令牌无效");
        }
    }
}
