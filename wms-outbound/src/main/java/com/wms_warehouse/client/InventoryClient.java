package com.wms_warehouse.client;

import com.wms_warehouse.common.api.AllocateItemRequest;
import com.wms_warehouse.common.api.AllocationSlice;
import com.wms_warehouse.common.api.PickItemRequest;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.function.Supplier;

/**
 * 库存服务内部接口（出库侧）：分配、拣货、释放整单占用。
 *
 * <p>出库服务不自己算 FEFO、也不自己写 {@code wms_allocation}——库存占用归库存中心。
 * 这里只把「这一行要多少」发过去，拿回「从哪几个批次各出多少」。
 */
@FeignClient(name = "wms-inventory", path = "/internal/inventory", configuration = InternalCallConfig.class)
public interface InventoryClient {

    @PostMapping("/allocate-item")
    Result<List<AllocationSlice>> allocateItem(@RequestBody AllocateItemRequest request);

    @PostMapping("/pick-item")
    Result<List<AllocationSlice>> pickItem(@RequestBody PickItemRequest request);

    @PostMapping("/release-order")
    Result<Integer> releaseOrder(@RequestBody PickItemRequest request);

    default List<AllocationSlice> allocateItemOrThrow(AllocateItemRequest request) {
        return unwrap(() -> allocateItem(request), "库存服务调用失败：");
    }

    default List<AllocationSlice> pickItemOrThrow(PickItemRequest request) {
        return unwrap(() -> pickItem(request), "库存服务调用失败：");
    }

    default int releaseOrderOrThrow(PickItemRequest request) {
        return unwrap(() -> releaseOrder(request), "库存服务调用失败：");
    }

    /**
     * 统一拆包：项目约定「HTTP 200 + body.code」，业务错误与远程故障在这里收敛成同一种异常，
     * 出库服务只管 catch BusinessException。
     */
    private <T> T unwrap(Supplier<Result<T>> call, String errorPrefix) {
        Result<T> result;
        try {
            result = call.get();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BizCode.BIZ_FAIL, errorPrefix + e.getMessage());
        }
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(result == null ? BizCode.BIZ_FAIL : result.getCode(),
                    result == null ? "库存服务无响应" : result.getMsg());
        }
        return result.getData();
    }
}
