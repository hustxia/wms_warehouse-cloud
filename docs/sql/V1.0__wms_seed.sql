-- ============================================================
-- V1.0 WMS 业务闭环：演示种子数据
--
-- 用途：给 S4 效果图与 S8 三层验证提供一套自洽数据。
-- 幂等：全部固定 id（1001+），ON DUPLICATE KEY UPDATE 保证可重复执行。
-- 与现有种子（id 1~8）互不干扰。
--
-- 场景设计（专为验收 FEFO 而造）
--   仓库 WH-DEMO 下设收货区 Z-RCV、存储区 Z-STO
--   SKU1001 批次管理、保质期 180 天；同库位两个批次：
--     B20261201  效期 2026-12-01  6 件   ← 效期近，FEFO 应先出
--     B20270601  效期 2027-06-01  4 件
--   合计 10 件，正好够验证「出库 8 件 = 6+2」「出库 12 件 = 部分分配」
-- ============================================================

-- ---------- 仓库 ----------
INSERT INTO wms_warehouse (id, warehouse_code, warehouse_name, address, contact_person, contact_phone, status, create_by, update_by, deleted)
VALUES (1001, 'WH-DEMO', '演示仓（V1.0 验收）', '郑州市经开区第八大街 1 号', '张仓管', '13800000001', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name);

-- ---------- 库区 ----------
INSERT INTO wms_zone (id, zone_code, zone_name, zone_type, warehouse_id, status, create_by, update_by, deleted)
VALUES (1001, 'Z-RCV', '收货暂存区', 'RECEIVING', 1001, 1, 1, 1, 0),
       (1002, 'Z-STO', '存储区', 'STORAGE', 1001, 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE zone_name = VALUES(zone_name);

-- ---------- 库位 ----------
INSERT INTO wms_location (id, location_code, location_type, warehouse_id, zone_id, max_capacity, is_mixed, status, create_by, update_by, deleted)
VALUES (1001, 'A-01-01', 'STORAGE', 1001, 1002, 1000, 0, 1, 1, 1, 0),
       (1002, 'A-01-02', 'STORAGE', 1001, 1002, 1000, 0, 1, 1, 1, 0),
       (1003, 'R-01-01', 'RECEIVING', 1001, 1001, 500, 1, 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE location_code = VALUES(location_code);

-- ---------- 货主 ----------
INSERT INTO wms_owner (id, owner_code, owner_name, contact, phone, status, create_by, update_by, deleted)
VALUES (1001, 'OW-DEMO', '演示货主（郑州优选贸易）', '李经理', '13800000002', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE owner_name = VALUES(owner_name);

-- ---------- SKU ----------
INSERT INTO wms_sku (id, sku_code, sku_name, spec, unit, is_batch, is_serial, shelf_life, barcode, status, create_by, update_by, deleted)
VALUES (1001, 'SKU-1001', '纯棉T恤 白色 L', '180g/m²，L 码', '件', 1, 0, 180, '6900000000011', 1, 1, 1, 0),
       (1002, 'SKU-1002', '帆布购物袋', '38×42cm，加厚', '个', 0, 0, NULL, '6900000000028', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE sku_name = VALUES(sku_name);

-- ---------- 库存（FEFO 验证用）----------
INSERT INTO wms_inventory (id, warehouse_id, owner_id, sku_id, location_id, batch_no, production_date, expiry_date, qty_on_hand, qty_allocated, qty_locked, version, create_by, update_by, deleted)
VALUES (1001, 1001, 1001, 1001, 1001, 'B20261201', '2026-06-01', '2026-12-01', 6, 0, 0, 0, 1, 1, 0),
       (1002, 1001, 1001, 1001, 1001, 'B20270601', '2026-12-01', '2027-06-01', 4, 0, 0, 0, 1, 1, 0),
       (1003, 1001, 1001, 1002, 1002, '',          NULL,         NULL,         50, 0, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE qty_on_hand = VALUES(qty_on_hand);

-- ---------- 演示入库单：3 行明细，含「1 行不带批次」----------
INSERT INTO wms_inbound_order (id, order_no, order_type, warehouse_id, owner_id, status, ref_no, create_by, update_by, deleted)
VALUES (1001, 'IB-DEMO-0001', 'PURCHASE', 1001, 1001, 'CREATED', 'V1.0-验收-入库', 1, 1, 0)
ON DUPLICATE KEY UPDATE status = 'CREATED';

INSERT INTO wms_inbound_order_item (id, order_id, line_no, sku_id, batch_no, expected_qty, received_qty, putaway_qty, location_id, status, create_by, update_by, deleted)
VALUES (1001, 1001, 1, 1001, 'B20280101', 20, 0, 0, 1001, 'PENDING', 1, 1, 0),
       (1002, 1001, 2, 1001, 'B20280201', 10, 0, 0, 1001, 'PENDING', 1, 1, 0),
       (1003, 1001, 3, 1002, '',          30, 0, 0, 1002, 'PENDING', 1, 1, 0)
ON DUPLICATE KEY UPDATE received_qty = 0, putaway_qty = 0, status = 'PENDING';

-- ---------- 演示出库单：1 行要 8 件（FEFO 应取 6+2）----------
INSERT INTO wms_outbound_order (id, order_no, order_type, warehouse_id, owner_id, status, ref_no, create_by, update_by, deleted)
VALUES (1001, 'OB-DEMO-0001', 'SALES', 1001, 1001, 'CREATED', 'V1.0-验收-出库', 1, 1, 0)
ON DUPLICATE KEY UPDATE status = 'CREATED';

INSERT INTO wms_outbound_order_item (id, order_id, line_no, sku_id, batch_no, order_qty, allocated_qty, picked_qty, shipped_qty, status, create_by, update_by, deleted)
VALUES (1001, 1001, 1, 1001, '', 8, 0, 0, 0, 'PENDING', 1, 1, 0)
ON DUPLICATE KEY UPDATE order_qty = 8, allocated_qty = 0, picked_qty = 0, shipped_qty = 0, status = 'PENDING';
