package com.wms_warehouse.client;

import com.wms_warehouse.common.api.LocationInfo;
import com.wms_warehouse.common.api.SkuInfo;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 主数据服务内部接口（上架时校验库位、推算效期）。
 */
@FeignClient(name = "wms-master", path = "/internal/master", configuration = InternalCallConfig.class)
public interface MasterClient {

    @GetMapping("/location/{id}")
    Result<LocationInfo> location(@PathVariable("id") Long id);

    @GetMapping("/sku/{id}")
    Result<SkuInfo> sku(@PathVariable("id") Long id);

    /** 库位必须存在且属于本仓，否则上架会把货记到别的仓库头上 */
    default LocationInfo requireLocation(Long locationId) {
        Result<LocationInfo> result = call(() -> location(locationId), "主数据服务调用失败：");
        if (result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            throw new BusinessException(result.getCode(), result.getMsg());
        }
        return result.getData();
    }

    /** SKU 查不到不阻断上架：效期只是排序因子，缺失时按「无效期」排最后 */
    default SkuInfo findSku(Long skuId) {
        try {
            Result<SkuInfo> result = sku(skuId);
            return result != null && result.getCode() == ResultCode.SUCCESS.getCode() ? result.getData() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private <T> Result<T> call(java.util.function.Supplier<Result<T>> supplier, String errorPrefix) {
        try {
            return supplier.get();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BizCode.BIZ_FAIL, errorPrefix + e.getMessage());
        }
    }
}
