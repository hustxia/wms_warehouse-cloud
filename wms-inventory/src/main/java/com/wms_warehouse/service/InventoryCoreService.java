package com.wms_warehouse.service;

import com.wms_warehouse.common.api.InventoryKey;
import com.wms_warehouse.entity.WmsInventory;
import com.wms_warehouse.entity.WmsInventoryTxn;

import java.math.BigDecimal;

/**
 * 库存变动唯一入口（WMS V1.0 P0-1）。
 *
 * <p><b>项目硬规则</b>：任何业务代码都不得直接 {@code updateById} 改 {@code qty_on_hand / qty_allocated / qty_locked}，
 * 必须走这里。绕过这个门，库存和流水就会脱节——那是「账面有货、实际没货」的根源，事后无法用数据自证。
 *
 * <p>实现约定：
 * <ol>
 *   <li>扣减/占用一律「条件 UPDATE + 检查影响行数」，不做先查后改；</li>
 *   <li>每个方法在同一事务内写一条 {@code wms_inventory_txn}，没有流水等于没发生；</li>
 *   <li>{@code qty_available} 是数据库生成列，任何方法都不写它。</li>
 * </ol>
 *
 * <p>关于流水口径：只有 {@link #increaseOnHand}/{@link #decreaseOnHand} 改变在库量，
 * 其流水的 {@code qty_change} 是真实增减量；分配/释放/冻结/解冻只改变占用，
 * 流水里记 {@code qty_change = 0} 并把实际占用量写进 {@code remark}。
 * 这样「在库量 = 该库存记录所有流水 qty_change 之和」这条恒等式始终成立（验收脚本第 6 条靠它）。
 */
public interface InventoryCoreService {

    /**
     * 增加在库量（收货/上架/盘盈）。库存记录不存在则按业务键新建，软删过的记录会被复活。
     *
     * @return 变动后的库存记录（含 id 与最新各数量）
     */
    WmsInventory increaseOnHand(InventoryKey key, BigDecimal qty, String txnType, String refNo, String remark);

    /**
     * 减少在库量（发运/盘亏/调拨出）。
     *
     * @throws com.wms_warehouse.common.exception.BusinessException 库存不足（含冻结量时按冻结后可用量判断）
     */
    void decreaseOnHand(Long inventoryId, BigDecimal qty, String txnType, String refNo, String remark);

    /** 分配占用（出库单分配）。占用不影响在库量，只影响可用量。 */
    void allocate(Long inventoryId, BigDecimal qty, String refNo);

    /**
     * 尝试分配：可用量不够时返回 {@code false} 而不是抛异常。
     *
     * <p>FEFO 分配要逐个候选库存试，抢不到就换下一条。这里必须是「返回值」语义：
     * 在同一个事务里让 {@code @Transactional} 方法抛异常，外层即使 catch 住，
     * 事务也已经被标记 rollback-only，提交时会变成 UnexpectedRollbackException——比原错误更难查。
     *
     * @return true 分配成功；false 可用量不足（未产生任何变更）
     */
    boolean tryAllocate(Long inventoryId, BigDecimal qty, String refNo);

    /** 释放分配占用（取消分配、拣货销账）。 */
    void releaseAllocate(Long inventoryId, BigDecimal qty, String refNo);

    /** 冻结（盘点/质检/调拨在途）。 */
    void lock(Long inventoryId, BigDecimal qty, String lockType, String refNo);

    /** 解冻。 */
    void unlock(Long inventoryId, BigDecimal qty, String lockType, String refNo);

    /**
     * V1.1 F-08 对账冲销：把账面在库量校正到 {@code targetOnHand}，并写一条 ADJUST 流水。
     *
     * <p><b>为什么不能拿「on_hand = Σ 全量流水」当对账口径</b>：seed 库存自带期初量，
     * 却没有对应的期初流水，干净库上就会报假差异。真正的恒等式是增量的：
     * 最近一条流水的 {@code qty_after} 必须等于当前 {@code qty_on_hand}。
     * 一旦有人绕过本服务直接改库（或跨服务远程提交成功、本地事务失败），
     * 这个等式就断，对账脚本据此报差异。
     *
     * <p>本方法写一条 {@code qty_change = target - before}、{@code qty_before/after = before/target}
     * 的流水，既满足「每条流水前后台量差 = qty_change」，也让最新流水重新与账面吻合。
     *
     * @return 本次写入的 ADJUST 流水（含 id，供冲销端点返回给运维）
     */
    WmsInventoryTxn reconcileAdjust(Long inventoryId, BigDecimal targetOnHand,
                                    String refNo, String remark);
}
