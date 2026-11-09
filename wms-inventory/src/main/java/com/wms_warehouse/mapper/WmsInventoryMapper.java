package com.wms_warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms_warehouse.common.api.InventoryKey;
import com.wms_warehouse.entity.WmsInventory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存表 Mapper
 */
@Mapper
public interface WmsInventoryMapper extends BaseMapper<WmsInventory> {

    // <gen:begin key="mapper-body">
    // <gen:end key="mapper-body">
    // ===== 以下为手写区：自定义方法写在这里，重新生成不会被覆盖 =====

    /**
     * 按业务键「增加在库量」：存在则累加，不存在则新建。
     *
     * <p>为什么用 {@code ON DUPLICATE KEY UPDATE} 而不是先查再插：唯一键 {@code uk_inv} 不含 {@code deleted}，
     * 软删过的行仍占着 key。先查会因为逻辑删除查不到而走 insert，然后撞唯一键。
     * 这里让数据库自己做冲突判定，并把 {@code deleted} 复位成 0——**复活**旧行，
     * 符合业务语义（同库位同批次本就该只有一条库存记录）。
     *
     * <p>复活时数量直接取本次数量（旧行的量已随软删视为不存在）；效期不覆盖，
     * 避免同批次重复收货把首次登记的生产日期改成后来的日期。
     */
    @Insert("""
            INSERT INTO wms_inventory
                (warehouse_id, owner_id, sku_id, location_id, batch_no, production_date, expiry_date,
                 qty_on_hand, qty_allocated, qty_locked, version, updated_at, create_time, deleted)
            VALUES
                (#{key.warehouseId}, #{key.ownerId}, #{key.skuId}, #{key.locationId}, #{key.batchNo},
                 #{key.productionDate,jdbcType=DATE}, #{key.expiryDate,jdbcType=DATE},
                 #{qty}, 0, 0, 0, NOW(), NOW(), 0)
            ON DUPLICATE KEY UPDATE
                qty_on_hand = CASE WHEN deleted = 1 THEN #{qty} ELSE qty_on_hand + #{qty} END,
                deleted     = 0,
                updated_at  = NOW()
            """)
    int upsertIncrease(@Param("key") InventoryKey key, @Param("qty") BigDecimal qty);

    /** 按业务键查有效库存记录（手写 SQL 不会自动追加逻辑删除条件，这里显式写） */
    @Select("""
            SELECT * FROM wms_inventory
            WHERE warehouse_id = #{key.warehouseId}
              AND owner_id     = #{key.ownerId}
              AND sku_id       = #{key.skuId}
              AND location_id  = #{key.locationId}
              AND batch_no     = #{key.batchNo}
              AND deleted      = 0
            """)
    WmsInventory selectByBizKey(@Param("key") InventoryKey key);

    /**
     * 减在库量：条件 UPDATE，影响行数 0 即库存不足。
     *
     * <p>判定里扣掉锁定量（{@code qty_on_hand - qty_locked >= qty}）：冻结的量不能再发货。
     * 绝不做「先 select 再 update」——那中间有窗口，就是超卖的经典写法。
     */
    @Update("""
            UPDATE wms_inventory
            SET qty_on_hand = qty_on_hand - #{qty},
                version     = version + 1,
                updated_at  = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND qty_on_hand - qty_locked >= #{qty}
            """)
    int decreaseOnHand(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 分配占用：可用量（在库-已分配-锁定）够才加锁 */
    @Update("""
            UPDATE wms_inventory
            SET qty_allocated = qty_allocated + #{qty},
                version       = version + 1,
                updated_at    = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND qty_on_hand - qty_allocated - qty_locked >= #{qty}
            """)
    int allocate(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 释放分配（取消分配 / 拣货后销账） */
    @Update("""
            UPDATE wms_inventory
            SET qty_allocated = qty_allocated - #{qty},
                version       = version + 1,
                updated_at    = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND qty_allocated >= #{qty}
            """)
    int releaseAllocate(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 冻结（盘点/质检/调拨）：锁定量计入可用量，冻结期间不可再分配 */
    @Update("""
            UPDATE wms_inventory
            SET qty_locked = qty_locked + #{qty},
                version    = version + 1,
                updated_at = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND qty_on_hand - qty_allocated - qty_locked >= #{qty}
            """)
    int lockQty(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 解冻 */
    @Update("""
            UPDATE wms_inventory
            SET qty_locked = qty_locked - #{qty},
                version    = version + 1,
                updated_at = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND qty_locked >= #{qty}
            """)
    int unlockQty(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * FEFO 候选库存：按效期先进先出。
     *
     * <p>排序三要素：效期近的优先 → 无效期的排最后（{@code CASE WHEN expiry_date IS NULL THEN 1}）
     * → 同效期按批次号、库位号稳定排序（避免同一次分配在两次执行里选中不同库位，排查困难）。
     */
    @Select("""
            SELECT * FROM wms_inventory
            WHERE deleted = 0
              AND warehouse_id = #{warehouseId}
              AND owner_id     = #{ownerId}
              AND sku_id       = #{skuId}
              AND qty_on_hand - qty_allocated - qty_locked > 0
            ORDER BY CASE WHEN expiry_date IS NULL THEN 1 ELSE 0 END,
                     expiry_date ASC,
                     batch_no ASC,
                     location_id ASC
            """)
    List<WmsInventory> selectAvailableForFefo(@Param("warehouseId") Long warehouseId,
                                              @Param("ownerId") Long ownerId,
                                              @Param("skuId") Long skuId);

    /**
     * V1.1 F-08 对账冲销：把账面在库量校正到目标值。
     *
     * <p>带 {@code qty_on_hand = #{before}} 的乐观条件：对账脚本读到差异后、
     * 运维点冲销之前，若现场又发生了一笔正常出入库，这次校正就不能覆盖别人的写入。
     * 影响行数为 0 时由 Service 报错，让运维重新对账。
     *
     * <p>这是「库存唯一入口」的合法成员——只有核心服务能改 on_hand，
     * 冲销也不例外；直接写 SQL 修库正是本版要消灭的做法。
     */
    @Update("""
            UPDATE wms_inventory
            SET qty_on_hand = #{target},
                version     = version + 1,
                updated_at  = NOW()
            WHERE id = #{id}
              AND deleted = 0
              AND qty_on_hand = #{before}
            """)
    int setToOnHand(@Param("id") Long id, @Param("before") BigDecimal before,
                    @Param("target") BigDecimal target);
}
