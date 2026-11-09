package com.wms_warehouse.client;

import com.wms_warehouse.common.api.InventoryIncreaseRequest;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务内部接口（入库侧只用「增加在库量」）。
 *
 * <p>为什么不直接注入库存 Mapper：微服务拆分后共库只是部署事实，不是设计许可。
 * 库存的写入口留在 wms-inventory 一个进程里，跨服务改库存只能过这个接口——
 * 「唯一入口」从代码约定升级成拓扑约束。
 */
@FeignClient(name = "wms-inventory", path = "/internal/inventory", configuration = InternalCallConfig.class)
public interface InventoryClient {

    @PostMapping("/increase")
    Result<Void> increase(@RequestBody InventoryIncreaseRequest request);

    /**
     * 增加在库量，失败抛业务异常。
     *
     * <p>把「HTTP 200 + body.code」的项目约定收在客户端里：调用方只关心成功/失败，
     * 不必每次手写 code 判断；库存不足这类业务错误也原样透传 message。
     */
    default void increaseOrThrow(InventoryIncreaseRequest request) {
        Result<Void> result;
        try {
            result = increase(request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BizCode.BIZ_FAIL, "库存服务调用失败：" + e.getMessage());
        }
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(result == null ? BizCode.BIZ_FAIL : result.getCode(),
                    result == null ? "库存服务无响应" : result.getMsg());
        }
    }
}
