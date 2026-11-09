package com.wms_warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms_warehouse.common.context.UserContext;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.statemachine.DocStateMachine;
import com.wms_warehouse.common.util.QtyUtils;
import com.wms_warehouse.dto.biz.CountRequest;
import com.wms_warehouse.client.MasterClient;
import com.wms_warehouse.common.api.InventoryKey;
import com.wms_warehouse.dto.biz.StocktakeCreateRequest;
import com.wms_warehouse.entity.WmsInventory;
import com.wms_warehouse.entity.WmsInventoryLock;
import com.wms_warehouse.entity.WmsStocktakeItem;
import com.wms_warehouse.entity.WmsStocktakeOrder;
import com.wms_warehouse.service.InventoryCoreService;
import com.wms_warehouse.common.idempotent.WmsIdempotentService;
import com.wms_warehouse.service.WmsInventoryLockService;
import com.wms_warehouse.service.WmsInventoryService;
import com.wms_warehouse.service.WmsStocktakeBizService;
import com.wms_warehouse.service.WmsStocktakeItemService;
import com.wms_warehouse.service.WmsStocktakeOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 盘点业务闭环实现。
 *
 * <p>一个容易被忽略的顺序问题：确认调整时必须**先解冻、再调整**。
 * 盘亏要减在库量，而减库存的条件是 {@code qty_on_hand - qty_locked >= qty}；
 * 冻结还在的话这个条件恒不成立，盘亏永远调不下去。
 */
@Slf4j
@Service
public class WmsStocktakeBizServiceImpl implements WmsStocktakeBizService {

    private static final String ACTION_CREATE = "STOCKTAKE_CREATE";
    private static final String ACTION_FREEZE = "STOCKTAKE_FREEZE";
    private static final String ACTION_COUNT = "STOCKTAKE_COUNT";
    private static final String ACTION_CONFIRM = "STOCKTAKE_CONFIRM";
    private static final String LOCK_TYPE = "STOCKTAKE";

    @Autowired
    private WmsStocktakeOrderService stocktakeOrderService;

    @Autowired
    private WmsStocktakeItemService stocktakeItemService;

    @Autowired
    private WmsInventoryService inventoryService;

    @Autowired
    private WmsInventoryLockService inventoryLockService;

    @Autowired
    private MasterClient masterClient;

    @Autowired
    private InventoryCoreService inventoryCoreService;

    @Autowired
    private WmsIdempotentService idempotentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(StocktakeCreateRequest request) {
        if (StringUtils.hasText(request.getRequestId())) {
            Map<String, Object> replay = idempotentService.beginOrReplay(
                    request.getRequestId(), ACTION_CREATE, "");
            if (replay != null) {
                return replay;
            }
        }

        List<WmsInventory> snapshot = snapshotInventories(request);
        if (snapshot.isEmpty()) {
            throw new BusinessException(BizCode.BIZ_FAIL, "所选范围内没有可盘点的库存（在库量为 0 的记录不纳入）");
        }

        WmsStocktakeOrder order = new WmsStocktakeOrder();
        order.setStocktakeNo(QtyUtils.nextNo("ST"));
        order.setWarehouseId(request.getWarehouseId());
        order.setZoneId(request.getZoneId());
        order.setStocktakeType(StringUtils.hasText(request.getStocktakeType()) ? request.getStocktakeType() : "全盘");
        order.setStatus("CREATED");
        order.setCreatedBy(UserContext.getUserId());
        order.setCreatedAt(LocalDateTime.now());
        stocktakeOrderService.save(order);

        for (WmsInventory inv : snapshot) {
            WmsStocktakeItem item = new WmsStocktakeItem();
            item.setStocktakeId(order.getId());
            item.setInventoryId(inv.getId());
            item.setSkuId(inv.getSkuId());
            item.setLocationId(inv.getLocationId());
            item.setBatchNo(inv.getBatchNo() == null ? "" : inv.getBatchNo());
            // 快照：之后库存再变也不影响本次差异
            item.setSystemQty(QtyUtils.nz(inv.getQtyOnHand()));
            item.setStatus("PENDING");
            stocktakeItemService.save(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stocktakeNo", order.getStocktakeNo());
        result.put("stocktakeId", String.valueOf(order.getId()));
        result.put("itemCount", snapshot.size());
        if (StringUtils.hasText(request.getRequestId())) {
            idempotentService.finish(request.getRequestId(), result);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> freeze(Long stocktakeId, String requestId) {
        WmsStocktakeOrder order = requireOrder(stocktakeId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                requestId, ACTION_FREEZE, order.getStocktakeNo());
        if (replay != null) {
            return replay;
        }
        DocStateMachine.require(DocStateMachine.STOCKTAKE, order.getStatus(), "冻结", "CREATED");

        List<WmsStocktakeItem> items = listItems(stocktakeId);
        int lockedCount = 0;
        for (WmsStocktakeItem item : items) {
            WmsInventory inv = inventoryService.getById(item.getInventoryId());
            if (inv == null) {
                // 库存记录已不存在（被清理），这张明细本次盘不到，跳过而不是让整单失败
                log.warn("盘点冻结跳过：库存记录不存在 inventoryId={}, stocktakeNo={}",
                        item.getInventoryId(), order.getStocktakeNo());
                continue;
            }
            // 只冻结当前「可用」部分：冻结已分配/已锁定的量没有意义，还会让解冻时数量对不上
            BigDecimal available = QtyUtils.nz(inv.getQtyOnHand())
                    .subtract(QtyUtils.nz(inv.getQtyAllocated()))
                    .subtract(QtyUtils.nz(inv.getQtyLocked()));
            if (available.signum() <= 0) {
                continue;
            }
            inventoryCoreService.lock(inv.getId(), available, LOCK_TYPE, order.getStocktakeNo());

            WmsInventoryLock lock = new WmsInventoryLock();
            lock.setLockNo(QtyUtils.nextNo("LK"));
            lock.setInventoryId(inv.getId());
            lock.setSkuId(inv.getSkuId());
            lock.setLocationId(inv.getLocationId());
            lock.setLockedQty(available);
            lock.setRefOrderNo(order.getStocktakeNo());
            lock.setLockType(LOCK_TYPE);
            lock.setStatus(1);
            lock.setCreatedAt(LocalDateTime.now());
            inventoryLockService.save(lock);
            lockedCount++;
        }

        DocStateMachine.move(DocStateMachine.STOCKTAKE, order.getStatus(), "FROZEN");
        order.setStatus("FROZEN");
        stocktakeOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stocktakeNo", order.getStocktakeNo());
        result.put("status", "FROZEN");
        result.put("lockedCount", lockedCount);
        idempotentService.finish(requestId, result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> count(Long itemId, CountRequest request) {
        WmsStocktakeItem item = stocktakeItemService.getById(itemId);
        if (item == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "盘点明细不存在");
        }
        WmsStocktakeOrder order = requireOrder(item.getStocktakeId());
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION_COUNT, order.getStocktakeNo());
        if (replay != null) {
            return replay;
        }
        // 未冻结就录实盘，等于在会变的账面上记账，差异没有意义
        DocStateMachine.require(DocStateMachine.STOCKTAKE, order.getStatus(), "录实盘", "FROZEN");

        BigDecimal counted = request.getCountedQty();
        if (counted == null || counted.signum() < 0) {
            throw new BusinessException(BizCode.BIZ_FAIL, "实盘数量不能为负数");
        }
        item.setCountedQty(counted);
        item.setStatus("COUNTED");
        item.setCountedBy(UserContext.getUserId());
        item.setCountedAt(LocalDateTime.now());
        // diff_qty 是数据库生成列，写它会报 3105；读出来的旧值必须清掉再更新
        item.setDiffQty(null);
        stocktakeItemService.updateById(item);

        BigDecimal diff = counted.subtract(QtyUtils.nz(item.getSystemQty()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", String.valueOf(item.getId()));
        result.put("systemQty", item.getSystemQty());
        result.put("countedQty", counted);
        result.put("diffQty", diff);
        result.put("status", item.getStatus());
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long stocktakeId, String requestId) {
        WmsStocktakeOrder order = requireOrder(stocktakeId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                requestId, ACTION_CONFIRM, order.getStocktakeNo());
        if (replay != null) {
            return replay;
        }
        DocStateMachine.require(DocStateMachine.STOCKTAKE, order.getStatus(), "确认调整", "FROZEN");

        // 第一步：先解冻。顺序反了盘亏永远扣不动（扣库存的条件里带着 locked 判定）
        List<WmsInventoryLock> locks = inventoryLockService.list(new LambdaQueryWrapper<WmsInventoryLock>()
                .eq(WmsInventoryLock::getRefOrderNo, order.getStocktakeNo())
                .eq(WmsInventoryLock::getStatus, 1));
        for (WmsInventoryLock lock : locks) {
            inventoryCoreService.unlock(lock.getInventoryId(), QtyUtils.nz(lock.getLockedQty()),
                    LOCK_TYPE, order.getStocktakeNo());
            lock.setStatus(0);
            lock.setReleasedAt(LocalDateTime.now());
            inventoryLockService.updateById(lock);
        }

        // 第二步：按快照差异调整在库量
        List<WmsStocktakeItem> items = listItems(stocktakeId);
        int adjustedCount = 0;
        int uncountedCount = 0;
        BigDecimal diffTotal = BigDecimal.ZERO;

        for (WmsStocktakeItem item : items) {
            if (item.getCountedQty() == null) {
                // 没盘的行不动账面，但也不能假装它被确认过
                uncountedCount++;
                continue;
            }
            BigDecimal diff = QtyUtils.nz(item.getCountedQty()).subtract(QtyUtils.nz(item.getSystemQty()));
            if (diff.signum() != 0) {
                WmsInventory inv = inventoryService.getById(item.getInventoryId());
                if (inv == null) {
                    throw new BusinessException(BizCode.NOT_FOUND, "库存记录不存在：" + item.getInventoryId());
                }
                if (diff.signum() > 0) {
                    InventoryKey key = new InventoryKey(inv.getWarehouseId(), inv.getOwnerId(), inv.getSkuId(),
                            inv.getLocationId(), inv.getBatchNo(), inv.getProductionDate(), inv.getExpiryDate());
                    inventoryCoreService.increaseOnHand(key, diff, "ADJUST", order.getStocktakeNo(),
                            "盘点盘盈，库位 " + item.getLocationId());
                } else {
                    BigDecimal loss = diff.negate();
                    if (QtyUtils.nz(inv.getQtyOnHand()).compareTo(loss) < 0) {
                        // 盘点期间被发运导致账面已低于实盘，硬调会调成负数——只能重新盘
                        throw new BusinessException(BizCode.BIZ_FAIL,
                                "盘亏 " + QtyUtils.plain(loss) + " 超过当前在库 " + QtyUtils.plain(inv.getQtyOnHand())
                                        + "（盘点期间库存已变动），请重新盘点");
                    }
                    inventoryCoreService.decreaseOnHand(inv.getId(), loss, "ADJUST", order.getStocktakeNo(),
                            "盘点盘亏，库位 " + item.getLocationId());
                }
                adjustedCount++;
                diffTotal = diffTotal.add(diff);
            }
            item.setStatus("CONFIRMED");
            item.setDiffQty(null);
            stocktakeItemService.updateById(item);
        }

        DocStateMachine.move(DocStateMachine.STOCKTAKE, order.getStatus(), "CONFIRMED");
        order.setStatus("CONFIRMED");
        order.setConfirmedAt(LocalDateTime.now());
        stocktakeOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stocktakeNo", order.getStocktakeNo());
        result.put("status", "CONFIRMED");
        result.put("adjustedCount", adjustedCount);
        result.put("uncountedCount", uncountedCount);
        result.put("diffTotal", diffTotal);
        idempotentService.finish(requestId, result);
        return result;
    }

    /** 按「库位 &gt; 库区 &gt; 全仓」的优先级取快照范围 */
    private List<WmsInventory> snapshotInventories(StocktakeCreateRequest request) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseId, request.getWarehouseId())
                .gt(WmsInventory::getQtyOnHand, BigDecimal.ZERO)
                .orderByAsc(WmsInventory::getLocationId)
                .orderByAsc(WmsInventory::getBatchNo);
        if (request.getLocationId() != null) {
            wrapper.eq(WmsInventory::getLocationId, request.getLocationId());
        } else if (request.getZoneId() != null) {
            // 库位归 wms-master，走内部接口取 ID 列表，不在库存服务里直查 wms_location
            List<Long> locationIds = masterClient.locationIdsOfZone(request.getZoneId());
            if (locationIds.isEmpty()) {
                throw new BusinessException(BizCode.BIZ_FAIL, "该库区下没有库位");
            }
            wrapper.in(WmsInventory::getLocationId, locationIds);
        }
        return inventoryService.list(wrapper);
    }

    private WmsStocktakeOrder requireOrder(Long stocktakeId) {
        WmsStocktakeOrder order = stocktakeOrderService.getById(stocktakeId);
        if (order == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "盘点单不存在");
        }
        return order;
    }

    private List<WmsStocktakeItem> listItems(Long stocktakeId) {
        List<WmsStocktakeItem> items = new ArrayList<>(stocktakeItemService.list(
                new LambdaQueryWrapper<WmsStocktakeItem>()
                        .eq(WmsStocktakeItem::getStocktakeId, stocktakeId)
                        .orderByAsc(WmsStocktakeItem::getId)));
        if (items.isEmpty()) {
            throw new BusinessException(BizCode.BIZ_FAIL, "盘点单没有明细行");
        }
        return items;
    }
}
