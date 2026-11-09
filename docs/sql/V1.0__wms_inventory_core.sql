-- ============================================================
-- V1.0 WMS 业务闭环：库存核心结构变更
--
-- 幂等：可重复执行（information_schema 判存在 + PREPARE 动态 DDL，
--       不使用 DELIMITER / 存储过程，JDBC 与 mysql 客户端都能跑）
--
-- 变更清单
--   1. wms_inventory.qty_available 生成列改口径：
--      旧：qty_on_hand - qty_allocated          （漏减 qty_locked → 盘点冻结期间仍可分配，超卖）
--      新：qty_on_hand - qty_allocated - qty_locked
--      MySQL 不支持就地改生成列表达式，只能 DROP + ADD（会重建表；当前表 0 行，秒级）
--   2. 新增 wms_request_log：业务端点幂等键表
--
-- 不做 DDL 的语义约定（写进代码，不落库）
--   wms_inventory_lock.lock_type 取值收敛为 STOCKTAKE / QC / TRANSFER，
--   去掉 PICK —— PICK 的占用语义已由 wms_inventory.qty_allocated 表达，
--   两个字段同时计会导致可用量被扣两次。
-- ============================================================

-- ---------- 1. qty_available 改口径 ----------
-- 1.1 若已有定义且未含 qty_locked，先删
SET @old_expr := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'wms_inventory'
      AND COLUMN_NAME  = 'qty_available'
      AND GENERATION_EXPRESSION LIKE '%qty_locked%'
);
SET @sql := IF(@old_expr = 0,
    'ALTER TABLE wms_inventory DROP COLUMN qty_available',
    'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.2 列不存在则按新口径重建
SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'wms_inventory'
      AND COLUMN_NAME  = 'qty_available'
);
SET @sql := IF(@has_col = 0,
    'ALTER TABLE wms_inventory ADD COLUMN qty_available DECIMAL(14,3) GENERATED ALWAYS AS (qty_on_hand - qty_allocated - qty_locked) STORED COMMENT ''可用数量=在库-已分配-锁定'' AFTER qty_locked',
    'DO 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 2. 幂等键表 ----------
CREATE TABLE IF NOT EXISTS wms_request_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    request_id  VARCHAR(64)  NOT NULL                COMMENT '客户端幂等键(UUID)',
    action      VARCHAR(32)  NOT NULL                COMMENT '动作:RECEIVE/PUTAWAY/ALLOCATE/PICK/SHIP/STOCKTAKE_CONFIRM',
    order_no    VARCHAR(64)  NOT NULL                COMMENT '关联单据号',
    result_json TEXT                                 COMMENT '首次执行结果快照，重复请求原样返回',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_request (request_id),
    KEY idx_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务端点幂等键';
