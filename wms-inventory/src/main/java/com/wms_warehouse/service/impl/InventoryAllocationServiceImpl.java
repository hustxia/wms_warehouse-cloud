package com.wms_warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms_warehouse.common.api.AllocateItemRequest;
import com.wms_warehouse.common.api.AllocationSlice;
import com.wms_warehouse.common.api.PickItemRequest;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.util.QtyUtils;
import com.wms_warehouse.entity.WmsAllocation;
import com.wms_warehouse.entity.WmsInventory;
import com.wms_warehouse.mapper.WmsInventoryMapper;
import com.wms_warehouse.service.InventoryAllocationService;
import com.wms_warehouse.service.InventoryCoreService;
import com.wms_warehouse.service.WmsAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分配/拣货在库存侧的实现。
 *
 * <p>FEFO 排序写在 SQL 里（{@code selectAvailableForFefo}），循环只负责「按顺序吃到够」。
 * 排序规则只留一处，业务层不再二次排序——两处排序早晚会不一致。
 */
@Slf4j
@Service
public class InventoryAllocationServiceImpl implements InventoryAllocationService {

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private WmsAllocationService allocationService;

    @Autowired
    private InventoryCoreService inventoryCoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AllocationSlice> allocate(AllocateItemRequest request) {
        BigDecimal need = QtyUtils.nz(request.getNeedQty());
        List<AllocationSlice> slices = new ArrayList<>();
        if (need.signum() <= 0) {
            return slices;
        }
        List<WmsInventory> candidates = inventoryMapper.selectAvailableForFefo(
                request.getWarehouseId(), request.getOwnerId(), request.getSkuId());
        for (WmsInventory inv : candidates) {
            if (need.signum() <= 0) {
                break;
            }
            BigDecimal available = QtyUtils.nz(inv.getQtyOnHand())
                    .subtract(QtyUtils.nz(inv.getQtyAllocated()))
                    .subtract(QtyUtils.nz(inv.getQtyLocked()));
            if (available.signum() <= 0) {
                continue;
            }
            BigDecimal take = available.min(need);
            // 抢不到说明被别的单占了（并发），换下一条候选，不报错
            if (!inventoryCoreService.tryAllocate(inv.getId(), take, request.getOrderNo())) {
                continue;
            }
            WmsAllocation allocation = new WmsAllocation();
            allocation.setOrderId(request.getOrderId());
            allocation.setItemId(request.getItemId());
            allocation.setInventoryId(inv.getId());
            allocation.setSkuId(request.getSkuId());
            allocation.setLocationId(inv.getLocationId());
            allocation.setBatchNo(inv.getBatchNo() == null ? "" : inv.getBatchNo());
            allocation.setAllocatedQty(take);
            allocation.setStatus(1);
            allocation.setCreatedAt(LocalDateTime.now());
            allocationService.save(allocation);

            slices.add(toSlice(allocation, take));
            need = need.subtract(take);
        }
        return slices;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AllocationSlice> pick(PickItemRequest request) {
        BigDecimal need = QtyUtils.nz(request.getQty());
        List<AllocationSlice> slices = new ArrayList<>();
        if (need.signum() <= 0) {
            return slices;
        }
        List<WmsAllocation> allocations = allocationService.list(new LambdaQueryWrapper<WmsAllocation>()
                .eq(WmsAllocation::getItemId, request.getItemId())
                .eq(WmsAllocation::getStatus, 1)
                .orderByAsc(WmsAllocation::getId));

        for (WmsAllocation allocation : allocations) {
            if (need.signum() <= 0) {
                break;
            }
            BigDecimal remain = QtyUtils.nz(allocation.getAllocatedQty());
            if (remain.signum() <= 0) {
                continue;
            }
            BigDecimal take = remain.min(need);
            // 先扣在库量、再释放占用：扣减条件里带 locked 判定，顺序反了会出现
            // 「占用已释放、在库量没扣成」的中间态
            inventoryCoreService.decreaseOnHand(allocation.getInventoryId(), take, "PICK",
                    request.getOrderNo(), "出库拣货，批次 " + allocation.getBatchNo());
            inventoryCoreService.releaseAllocate(allocation.getInventoryId(), take, request.getOrderNo());

            allocation.setAllocatedQty(remain.subtract(take));
            if (allocation.getAllocatedQty().signum() <= 0) {
                allocation.setStatus(0);
            }
            allocationService.updateById(allocation);

            slices.add(toSlice(allocation, take));
            need = need.subtract(take);
        }
        if (need.signum() > 0) {
            // 分配明细被消耗（如取消分配后重拣），必须让用户重新分配，不能按 0 糊过去
            throw new BusinessException(BizCode.BIZ_FAIL, "分配不足，还差 " + QtyUtils.plain(need) + "，请重新分配");
        }
        return slices;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int releaseOrder(Long orderId, String orderNo) {
        List<WmsAllocation> allocations = allocationService.list(new LambdaQueryWrapper<WmsAllocation>()
                .eq(WmsAllocation::getOrderId, orderId)
                .eq(WmsAllocation::getStatus, 1));
        for (WmsAllocation allocation : allocations) {
            inventoryCoreService.releaseAllocate(allocation.getInventoryId(),
                    QtyUtils.nz(allocation.getAllocatedQty()), orderNo);
            allocation.setStatus(0);
            allocationService.updateById(allocation);
        }
        return allocations.size();
    }

    private AllocationSlice toSlice(WmsAllocation allocation, BigDecimal qty) {
        AllocationSlice slice = new AllocationSlice();
        slice.setAllocationId(allocation.getId());
        slice.setInventoryId(allocation.getInventoryId());
        slice.setLocationId(allocation.getLocationId());
        slice.setBatchNo(allocation.getBatchNo());
        slice.setQty(qty);
        return slice;
    }
}
