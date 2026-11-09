package com.wms_warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms_warehouse.client.InventoryClient;
import com.wms_warehouse.common.api.AllocateItemRequest;
import com.wms_warehouse.common.api.AllocationSlice;
import com.wms_warehouse.common.api.PickItemRequest;
import com.wms_warehouse.common.context.UserContext;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.idempotent.WmsIdempotentService;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.statemachine.DocStateMachine;
import com.wms_warehouse.common.util.QtyUtils;
import com.wms_warehouse.dto.biz.AllocateRequest;
import com.wms_warehouse.dto.biz.PickRequest;
import com.wms_warehouse.dto.biz.ShipRequest;
import com.wms_warehouse.entity.WmsOutboundOrder;
import com.wms_warehouse.entity.WmsOutboundOrderItem;
import com.wms_warehouse.entity.WmsPickTask;
import com.wms_warehouse.entity.WmsShipment;
import com.wms_warehouse.service.WmsOutboundBizService;
import com.wms_warehouse.service.WmsOutboundOrderItemService;
import com.wms_warehouse.service.WmsOutboundOrderService;
import com.wms_warehouse.service.WmsPickTaskService;
import com.wms_warehouse.service.WmsShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 出库业务闭环实现（微服务版）。
 *
 * <p><b>职责边界</b>：本服务只管单据状态、单据数量字段、拣货任务与发货单；
 * 库存占用与扣减全部交给 wms-inventory（{@link InventoryClient}）。
 * 分配策略（FEFO）也在库存侧——换策略不该动出库服务的代码。
 *
 * <p><b>事务边界</b>：本地事务只覆盖出库单相关表，远程库存调用是独立提交。
 * 采用「先校验本地 → 再远程 → 最后落本地」的顺序，远程失败即整体回滚；
 * 极端情况下（远程已提交、本地提交失败）会有库存与单据不符，靠
 * 「在库量 = 流水 qty_change 之和」对账发现，用盘点/冲销修正。
 */
@Slf4j
@Service
public class WmsOutboundBizServiceImpl implements WmsOutboundBizService {

    private static final String ACTION_ALLOCATE = "ALLOCATE";
    private static final String ACTION_PICK = "PICK";
    private static final String ACTION_SHIP = "SHIP";
    private static final String ACTION_CANCEL_ALLOCATE = "CANCEL_ALLOCATE";

    @Autowired
    private WmsOutboundOrderService outboundOrderService;

    @Autowired
    private WmsOutboundOrderItemService outboundOrderItemService;

    @Autowired
    private WmsPickTaskService pickTaskService;

    @Autowired
    private WmsShipmentService shipmentService;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private WmsIdempotentService idempotentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> allocate(Long orderId, AllocateRequest request) {
        WmsOutboundOrder order = requireOrder(orderId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION_ALLOCATE, order.getOrderNo());
        if (replay != null) {
            return replay;
        }
        DocStateMachine.require(DocStateMachine.OUTBOUND, order.getStatus(), "分配", "CREATED", "PARTIAL");

        Map<Long, WmsOutboundOrderItem> items = loadItems(orderId);
        List<Map<String, Object>> itemResults = new ArrayList<>();
        boolean anyAllocated = false;
        boolean allSatisfied = true;

        for (WmsOutboundOrderItem item : items.values()) {
            BigDecimal orderQty = QtyUtils.nz(item.getOrderQty());
            BigDecimal need = orderQty.subtract(QtyUtils.nz(item.getAllocatedQty()));
            List<Map<String, Object>> slices = new ArrayList<>();

            if (need.signum() > 0) {
                AllocateItemRequest allocateItem = new AllocateItemRequest();
                allocateItem.setOrderId(orderId);
                allocateItem.setOrderNo(order.getOrderNo());
                allocateItem.setWarehouseId(order.getWarehouseId());
                allocateItem.setOwnerId(order.getOwnerId());
                allocateItem.setItemId(item.getId());
                allocateItem.setSkuId(item.getSkuId());
                allocateItem.setNeedQty(need);

                for (AllocationSlice slice : inventoryClient.allocateItemOrThrow(allocateItem)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("inventoryId", String.valueOf(slice.getInventoryId()));
                    row.put("batchNo", slice.getBatchNo());
                    row.put("locationId", String.valueOf(slice.getLocationId()));
                    row.put("qty", slice.getQty());
                    slices.add(row);
                    need = need.subtract(QtyUtils.nz(slice.getQty()));
                }
            }

            BigDecimal allocated = orderQty.subtract(need);
            item.setAllocatedQty(allocated);
            item.setStatus(allocated.compareTo(orderQty) >= 0 ? "DONE"
                    : allocated.signum() > 0 ? "PARTIAL" : "PENDING");
            outboundOrderItemService.updateById(item);

            anyAllocated = anyAllocated || allocated.signum() > 0;
            allSatisfied = allSatisfied && "DONE".equals(item.getStatus());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", String.valueOf(item.getId()));
            row.put("allocatedQty", allocated);
            row.put("shortageQty", need.signum() > 0 ? need : BigDecimal.ZERO);
            row.put("allocations", slices);
            itemResults.add(row);
        }

        String next = allSatisfied ? "ALLOCATED" : anyAllocated ? "PARTIAL" : "CREATED";
        DocStateMachine.move(DocStateMachine.OUTBOUND, order.getStatus(), next);
        order.setStatus(next);
        outboundOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", next);
        result.put("items", itemResults);
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pick(Long orderId, PickRequest request) {
        WmsOutboundOrder order = requireOrder(orderId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION_PICK, order.getOrderNo());
        if (replay != null) {
            return replay;
        }
        DocStateMachine.require(DocStateMachine.OUTBOUND, order.getStatus(), "拣货",
                "ALLOCATED", "PARTIAL", "PICKING");

        Map<Long, WmsOutboundOrderItem> items = loadItems(orderId);
        BigDecimal totalPicked = BigDecimal.ZERO;

        for (PickRequest.Item reqItem : request.getItems()) {
            WmsOutboundOrderItem item = items.get(reqItem.getItemId());
            if (item == null) {
                throw new BusinessException(BizCode.NOT_FOUND, "出库单明细不存在：" + reqItem.getItemId());
            }
            BigDecimal qty = QtyUtils.requirePositive(reqItem.getPickedQty(), "拣货数量");
            BigDecimal allocated = QtyUtils.nz(item.getAllocatedQty());
            BigDecimal pickedAlready = QtyUtils.nz(item.getPickedQty());
            if (pickedAlready.add(qty).compareTo(allocated) > 0) {
                throw new BusinessException(BizCode.BIZ_FAIL,
                        "第 " + item.getLineNo() + " 行拣货数量超过已分配数量（已分配 "
                                + QtyUtils.plain(allocated) + "，已拣 " + QtyUtils.plain(pickedAlready) + "）");
            }

            PickItemRequest pickItem = new PickItemRequest();
            pickItem.setOrderId(orderId);
            pickItem.setOrderNo(order.getOrderNo());
            pickItem.setItemId(item.getId());
            pickItem.setQty(qty);
            List<AllocationSlice> slices = inventoryClient.pickItemOrThrow(pickItem);

            for (AllocationSlice slice : slices) {
                WmsPickTask task = new WmsPickTask();
                task.setTaskNo(QtyUtils.nextNo("PK"));
                task.setOrderId(orderId);
                task.setItemId(item.getId());
                task.setAllocationId(slice.getAllocationId());
                task.setSkuId(item.getSkuId());
                task.setLocationId(slice.getLocationId());
                task.setBatchNo(slice.getBatchNo() == null ? "" : slice.getBatchNo());
                task.setTaskQty(slice.getQty());
                task.setPickedQty(slice.getQty());
                task.setStatus("DONE");
                task.setAssigneeId(UserContext.getUserId());
                task.setCreatedAt(LocalDateTime.now());
                task.setFinishedAt(LocalDateTime.now());
                pickTaskService.save(task);
            }

            item.setPickedQty(pickedAlready.add(qty));
            item.setStatus(item.getPickedQty().compareTo(QtyUtils.nz(item.getOrderQty())) >= 0 ? "DONE" : "PARTIAL");
            outboundOrderItemService.updateById(item);
            totalPicked = totalPicked.add(qty);
        }

        boolean allPicked = items.values().stream()
                .allMatch(i -> QtyUtils.nz(i.getPickedQty()).compareTo(QtyUtils.nz(i.getOrderQty())) >= 0);
        String next = allPicked ? "PICKED" : "PICKING";
        DocStateMachine.move(DocStateMachine.OUTBOUND, order.getStatus(), next);
        order.setStatus(next);
        outboundOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", next);
        result.put("pickedQty", totalPicked);
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ship(Long orderId, ShipRequest request) {
        WmsOutboundOrder order = requireOrder(orderId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION_SHIP, order.getOrderNo());
        if (replay != null) {
            return replay;
        }
        // 只允许「已拣货」发运：部分拣货就发运，等于让客户替仓库背缺货
        DocStateMachine.require(DocStateMachine.OUTBOUND, order.getStatus(), "发运", "PICKED");

        Map<Long, WmsOutboundOrderItem> items = loadItems(orderId);
        for (WmsOutboundOrderItem item : items.values()) {
            item.setShippedQty(QtyUtils.nz(item.getPickedQty()));
            outboundOrderItemService.updateById(item);
        }

        WmsShipment shipment = new WmsShipment();
        shipment.setShipmentNo(QtyUtils.nextNo("SH"));
        shipment.setOrderId(orderId);
        shipment.setCarrier(request.getCarrier());
        shipment.setTrackingNo(request.getTrackingNo());
        shipment.setPackageCount(1);
        shipment.setStatus("SHIPPED");
        shipment.setShippedAt(LocalDateTime.now());
        shipment.setCreatedAt(LocalDateTime.now());
        shipmentService.save(shipment);

        DocStateMachine.move(DocStateMachine.OUTBOUND, order.getStatus(), "SHIPPED");
        order.setStatus("SHIPPED");
        order.setCarrier(request.getCarrier());
        order.setTrackingNo(request.getTrackingNo());
        outboundOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", "SHIPPED");
        result.put("shipmentNo", shipment.getShipmentNo());
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelAllocate(Long orderId, String requestId) {
        WmsOutboundOrder order = requireOrder(orderId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                requestId, ACTION_CANCEL_ALLOCATE, order.getOrderNo());
        if (replay != null) {
            return replay;
        }
        DocStateMachine.require(DocStateMachine.OUTBOUND, order.getStatus(), "取消分配", "ALLOCATED", "PARTIAL");

        PickItemRequest release = new PickItemRequest();
        release.setOrderId(orderId);
        release.setOrderNo(order.getOrderNo());
        int released = inventoryClient.releaseOrderOrThrow(release);

        Map<Long, WmsOutboundOrderItem> items = loadItems(orderId);
        for (WmsOutboundOrderItem item : items.values()) {
            item.setAllocatedQty(BigDecimal.ZERO);
            item.setStatus("PENDING");
            outboundOrderItemService.updateById(item);
        }

        DocStateMachine.move(DocStateMachine.OUTBOUND, order.getStatus(), "CREATED");
        order.setStatus("CREATED");
        outboundOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", "CREATED");
        result.put("releasedCount", released);
        idempotentService.finish(requestId, result);
        return result;
    }

    private WmsOutboundOrder requireOrder(Long orderId) {
        WmsOutboundOrder order = outboundOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "出库单不存在");
        }
        return order;
    }

    private Map<Long, WmsOutboundOrderItem> loadItems(Long orderId) {
        List<WmsOutboundOrderItem> list = outboundOrderItemService.list(
                new LambdaQueryWrapper<WmsOutboundOrderItem>()
                        .eq(WmsOutboundOrderItem::getOrderId, orderId)
                        .orderByAsc(WmsOutboundOrderItem::getLineNo));
        if (list.isEmpty()) {
            throw new BusinessException(BizCode.BIZ_FAIL, "出库单没有明细行");
        }
        return list.stream().collect(Collectors.toMap(WmsOutboundOrderItem::getId, Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
    }
}
