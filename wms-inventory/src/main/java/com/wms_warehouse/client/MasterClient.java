package com.wms_warehouse.client;

import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 主数据服务内部接口（库存侧只用「按库区取库位」：盘点按库区圈定快照范围）。
 *
 * <p>库存服务不直接查 {@code wms_location}：库位口径归 wms-master，
 * 跨服务读表等于把主数据的规则复制一份到库存服务里，改口径时必然漏改。
 */
@FeignClient(name = "wms-master", path = "/internal/master", configuration = InternalCallConfig.class)
public interface MasterClient {

    @GetMapping("/zone/{zoneId}/locations")
    Result<List<Long>> zoneLocations(@PathVariable("zoneId") Long zoneId);

    default List<Long> locationIdsOfZone(Long zoneId) {
        Result<List<Long>> result;
        try {
            result = zoneLocations(zoneId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BizCode.BIZ_FAIL, "主数据服务调用失败：" + e.getMessage());
        }
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(result == null ? BizCode.BIZ_FAIL : result.getCode(),
                    result == null ? "主数据服务无响应" : result.getMsg());
        }
        return result.getData() == null ? List.of() : result.getData();
    }
}
