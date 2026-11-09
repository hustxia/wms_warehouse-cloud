package com.wms_warehouse.service.impl;

import com.wms_warehouse.common.context.UserContext;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.util.QtyUtils;
import com.wms_warehouse.common.api.InventoryKey;
import com.wms_warehouse.entity.WmsInventory;
import com.wms_warehouse.entity.WmsInventoryTxn;
import com.wms_warehouse.mapper.WmsInventoryMapper;
import com.wms_warehouse.mapper.WmsInventoryTxnMapper;
import com.wms_warehouse.service.InventoryCoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动唯一入口实现。
 *
 * <p>方法全部 {@code @Transactional}：数量变更和流水要么一起落库，要么一起回滚。
 * 事务传播用默认 REQUIRED —— 业务端点（收货/拣货/盘点确认）在外层事务里调用时，
 * 库存变动与单据状态、分配明细在同一个事务里提交，不会出现「库存变了但单据没变」。
 */
@Slf4j
@Service
public class InventoryCoreServiceImpl implements InventoryCoreService {

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private WmsInventoryTxnMapper txnMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsInventory increaseOnHand(InventoryKey key, BigDecimal qty, String txnType, String refNo, String remark) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "变动数量");
        requireKey(key);
        inventoryMapper.upsertIncrease(key, delta);
        WmsInventory inv = inventoryMapper.selectByBizKey(key);
        if (inv == null) {
            throw new BusinessException(BizCode.BIZ_FAIL, "库存记录写入失败（业务键：" + key + "）");
        }
        BigDecimal after = QtyUtils.nz(inv.getQtyOnHand());
        writeTxn(inv, delta, after.subtract(delta), after, txnType, refNo, remark);
        return inv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseOnHand(Long inventoryId, BigDecimal qty, String txnType, String refNo, String remark) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "变动数量");
        if (inventoryMapper.decreaseOnHand(inventoryId, delta) == 0) {
            throw new BusinessException(BizCode.BIZ_FAIL, "库存不足：" + shortfallMessage(inventoryId, delta));
        }
        WmsInventory inv = requireInventory(inventoryId);
        BigDecimal after = QtyUtils.nz(inv.getQtyOnHand());
        writeTxn(inv, delta.negate(), after.add(delta), after, txnType, refNo, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allocate(Long inventoryId, BigDecimal qty, String refNo) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "分配数量");
        if (!tryAllocate(inventoryId, delta, refNo)) {
            throw new BusinessException(BizCode.BIZ_FAIL, "可用库存不足，无法分配：" + shortfallMessage(inventoryId, delta));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryAllocate(Long inventoryId, BigDecimal qty, String refNo) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "分配数量");
        if (inventoryMapper.allocate(inventoryId, delta) == 0) {
            return false;
        }
        writeOccupancyTxn(inventoryId, "ALLOCATE", refNo, "分配占用 " + QtyUtils.plain(delta));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseAllocate(Long inventoryId, BigDecimal qty, String refNo) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "释放数量");
        if (inventoryMapper.releaseAllocate(inventoryId, delta) == 0) {
            throw new BusinessException(BizCode.BIZ_FAIL, "释放分配失败：已分配数量不足 " + QtyUtils.plain(delta));
        }
        writeOccupancyTxn(inventoryId, "RELEASE_ALLOCATE", refNo, "释放占用 " + QtyUtils.plain(delta));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lock(Long inventoryId, BigDecimal qty, String lockType, String refNo) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "冻结数量");
        if (inventoryMapper.lockQty(inventoryId, delta) == 0) {
            throw new BusinessException(BizCode.BIZ_FAIL, "冻结失败：可用量不足，无法冻结 " + QtyUtils.plain(delta));
        }
        writeOccupancyTxn(inventoryId, "LOCK", refNo, "冻结(" + lockType + ") " + QtyUtils.plain(delta));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlock(Long inventoryId, BigDecimal qty, String lockType, String refNo) {
        BigDecimal delta = QtyUtils.requirePositive(qty, "解冻数量");
        if (inventoryMapper.unlockQty(inventoryId, delta) == 0) {
            throw new BusinessException(BizCode.BIZ_FAIL, "解冻失败：锁定量不足 " + QtyUtils.plain(delta));
        }
        writeOccupancyTxn(inventoryId, "UNLOCK", refNo, "解冻(" + lockType + ") " + QtyUtils.plain(delta));
    }

    /**
     * 只改变占用（分配/冻结）的流水：{@code qty_change = 0}，在库量前后相等。
     *
     * <p>这样记是为了满足两条同时成立的口径：一是「每次库存动作都有流水」可审计，
     * 二是「在库量 = 流水 qty_change 之和」不被占用动作破坏。
     */
    private void writeOccupancyTxn(Long inventoryId, String txnType, String refNo, String remark) {
        WmsInventory inv = requireInventory(inventoryId);
        BigDecimal onHand = QtyUtils.nz(inv.getQtyOnHand());
        writeTxn(inv, BigDecimal.ZERO, onHand, onHand, txnType, refNo, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsInventoryTxn reconcileAdjust(Long inventoryId, BigDecimal targetOnHand,
                                           String refNo, String remark) {
        if (targetOnHand == null || targetOnHand.signum() < 0) {
            throw new BusinessException(BizCode.BIZ_FAIL, "冲销目标在库量不能为空或为负");
        }
        WmsInventory inv = requireInventory(inventoryId);
        BigDecimal before = QtyUtils.nz(inv.getQtyOnHand());
        if (before.compareTo(targetOnHand) != 0
                && inventoryMapper.setToOnHand(inventoryId, before, targetOnHand) == 0) {
            throw new BusinessException(BizCode.BIZ_FAIL,
                    "冲销未执行：库存记录不存在或账面已被其他操作改动，请重新对账");
        }
        BigDecimal change = targetOnHand.subtract(before);
        String text = (remark == null || remark.isBlank())
                ? "对账冲销 " + QtyUtils.plain(change) : remark;
        return writeTxn(inv, change, before, targetOnHand, "ADJUST", refNo, text);
    }

    private WmsInventoryTxn writeTxn(WmsInventory inv, BigDecimal change, BigDecimal before, BigDecimal after,
                                     String txnType, String refNo, String remark) {
        WmsInventoryTxn txn = new WmsInventoryTxn();
        txn.setTxnNo(QtyUtils.nextNo("TX"));
        txn.setTxnType(txnType);
        txn.setWarehouseId(inv.getWarehouseId());
        txn.setOwnerId(inv.getOwnerId());
        txn.setSkuId(inv.getSkuId());
        txn.setLocationId(inv.getLocationId());
        txn.setBatchNo(inv.getBatchNo() == null ? "" : inv.getBatchNo());
        txn.setQtyChange(change);
        txn.setQtyBefore(before);
        txn.setQtyAfter(after);
        txn.setRefOrderNo(refNo);
        txn.setOperatorId(UserContext.getUserId());
        txn.setRemark(remark);
        txn.setCreatedAt(LocalDateTime.now());
        txnMapper.insert(txn);
        return txn;
    }

    private WmsInventory requireInventory(Long inventoryId) {
        WmsInventory inv = inventoryMapper.selectById(inventoryId);
        if (inv == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "库存记录不存在：" + inventoryId);
        }
        return inv;
    }

    /** 拼「可用 X，需要 Y」——只给「库存不足」四个字，仓库现场没法判断是该补货还是该等解冻 */
    private String shortfallMessage(Long inventoryId, BigDecimal need) {
        WmsInventory inv = inventoryMapper.selectById(inventoryId);
        if (inv == null) {
            return "库存记录不存在";
        }
        BigDecimal available = QtyUtils.nz(inv.getQtyOnHand())
                .subtract(QtyUtils.nz(inv.getQtyAllocated()))
                .subtract(QtyUtils.nz(inv.getQtyLocked()));
        return "可用 " + QtyUtils.plain(available) + "，需要 " + QtyUtils.plain(need);
    }

    private void requireKey(InventoryKey key) {
        if (key == null || key.getWarehouseId() == null || key.getOwnerId() == null
                || key.getSkuId() == null || key.getLocationId() == null) {
            throw new BusinessException(BizCode.BIZ_FAIL, "库存业务键不完整（仓库/货主/SKU/库位均不能为空）");
        }
        if (key.getBatchNo() == null) {
            key.setBatchNo("");
        }
    }
}
