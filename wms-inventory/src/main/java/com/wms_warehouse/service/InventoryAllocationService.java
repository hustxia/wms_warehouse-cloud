package com.wms_warehouse.service;

import com.wms_warehouse.common.api.AllocateItemRequest;
import com.wms_warehouse.common.api.AllocationSlice;
import com.wms_warehouse.common.api.PickItemRequest;

import java.util.List;

/**
 * 库存分配（库存中心的职责，不是出库服务的）。
 *
 * <p>为什么分配放在库存服务：分配改的是 {@code qty_allocated} 和 {@code wms_allocation} 明细，
 * 两者都归库存中心所有。出库服务只决定「这一行要 8 件」，至于从哪个批次、哪个库位出，
 * 是库存的策略问题（本版 FEFO），换策略不该动出库服务的代码。
 */
public interface InventoryAllocationService {

    /**
     * 按 FEFO 给一行单据分配库存，能分多少分多少（不足不报错，由调用方按返回量判断）。
     *
     * @return 每个被占用库存的切片；空列表表示完全没有可用库存
     */
    List<AllocationSlice> allocate(AllocateItemRequest request);

    /**
     * 拣货：扣在库量 + 释放占用 + 更新分配明细余额。
     *
     * @return 实际拣货切片，出库服务据此写拣货任务
     * @throws com.wms_warehouse.common.exception.BusinessException 分配明细不足（需要重新分配）
     */
    List<AllocationSlice> pick(PickItemRequest request);

    /**
     * 释放整单占用（取消分配）。
     *
     * @return 释放的分配明细条数
     */
    int releaseOrder(Long orderId, String orderNo);
}
