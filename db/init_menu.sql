-- ============================================================
-- 代码生成器产出的菜单注册 SQL（三级结构：系统 → 业务域 → 页面）
-- 目标工程需存在 sys_menu / sys_role_menu（字段：parent_id,name,path,component,perms,type,sort,status,deleted）
-- 生成时间：2026-11-05 09:25:33
-- 父菜单 ID：0    授权角色 ID：1
-- ============================================================

START TRANSACTION;

-- 【一级】wms 系统
SET @dir_sys_wms_warehouse = (SELECT id FROM sys_menu WHERE parent_id = 0 AND name = 'wms 系统' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT 0, 'wms 系统', '/wms_warehouse', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_sys_wms_warehouse IS NULL;
SET @dir_sys_wms_warehouse = COALESCE(@dir_sys_wms_warehouse, LAST_INSERT_ID());

-- 【二级】基础资料
SET @dir_grp_base = (SELECT id FROM sys_menu WHERE parent_id = @dir_sys_wms_warehouse AND name = '基础资料' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT @dir_sys_wms_warehouse, '基础资料', '/wms_warehouse/base', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_grp_base IS NULL;
SET @dir_grp_base = COALESCE(@dir_grp_base, LAST_INSERT_ID());

-- 【二级】其他
SET @dir_grp_other = (SELECT id FROM sys_menu WHERE parent_id = @dir_sys_wms_warehouse AND name = '其他' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT @dir_sys_wms_warehouse, '其他', '/wms_warehouse/other', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_grp_other IS NULL;
SET @dir_grp_other = COALESCE(@dir_grp_other, LAST_INSERT_ID());

-- 【二级】产品管理
SET @dir_grp_product = (SELECT id FROM sys_menu WHERE parent_id = @dir_sys_wms_warehouse AND name = '产品管理' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT @dir_sys_wms_warehouse, '产品管理', '/wms_warehouse/product', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_grp_product IS NULL;
SET @dir_grp_product = COALESCE(@dir_grp_product, LAST_INSERT_ID());

-- 【二级】库存管理
SET @dir_grp_stock = (SELECT id FROM sys_menu WHERE parent_id = @dir_sys_wms_warehouse AND name = '库存管理' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT @dir_sys_wms_warehouse, '库存管理', '/wms_warehouse/stock', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_grp_stock IS NULL;
SET @dir_grp_stock = COALESCE(@dir_grp_stock, LAST_INSERT_ID());

-- 【二级】入库管理
SET @dir_grp_inbound = (SELECT id FROM sys_menu WHERE parent_id = @dir_sys_wms_warehouse AND name = '入库管理' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT @dir_sys_wms_warehouse, '入库管理', '/wms_warehouse/inbound', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_grp_inbound IS NULL;
SET @dir_grp_inbound = COALESCE(@dir_grp_inbound, LAST_INSERT_ID());

-- 【二级】出库管理
SET @dir_grp_outbound = (SELECT id FROM sys_menu WHERE parent_id = @dir_sys_wms_warehouse AND name = '出库管理' AND type = 1 AND deleted = 0 LIMIT 1);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
SELECT @dir_sys_wms_warehouse, '出库管理', '/wms_warehouse/outbound', NULL, NULL, 1, 1, 1, 0 FROM DUAL
WHERE @dir_grp_outbound IS NULL;
SET @dir_grp_outbound = COALESCE(@dir_grp_outbound, LAST_INSERT_ID());

-- ===== 三级：页面 =====
-- wms 系统 / 基础资料 / 仓库主表（wms_warehouse）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_base, '仓库主表', '/wms_warehouse/base/wms_warehouse/list', 'wms_warehouse/index', 'wms_warehouse_wms_warehouse:list', 2, 1, 1, 0);
SET @menu_wms_warehouse = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_warehouse, '新增', NULL, NULL, 'wms_warehouse_wms_warehouse:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_warehouse, '修改', NULL, NULL, 'wms_warehouse_wms_warehouse:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_warehouse, '删除', NULL, NULL, 'wms_warehouse_wms_warehouse:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_warehouse, 0);

-- wms 系统 / 其他 / 库区表（wms_zone）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_other, '库区表', '/wms_warehouse/other/wms_zone/list', 'wms_zone/index', 'wms_warehouse_wms_zone:list', 2, 1, 1, 0);
SET @menu_wms_zone = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_zone, '新增', NULL, NULL, 'wms_warehouse_wms_zone:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_zone, '修改', NULL, NULL, 'wms_warehouse_wms_zone:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_zone, '删除', NULL, NULL, 'wms_warehouse_wms_zone:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_zone, 0);

-- wms 系统 / 基础资料 / 库位表（wms_location）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_base, '库位表', '/wms_warehouse/base/wms_location/list', 'wms_location/index', 'wms_warehouse_wms_location:list', 2, 1, 1, 0);
SET @menu_wms_location = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_location, '新增', NULL, NULL, 'wms_warehouse_wms_location:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_location, '修改', NULL, NULL, 'wms_warehouse_wms_location:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_location, '删除', NULL, NULL, 'wms_warehouse_wms_location:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_location, 0);

-- wms 系统 / 产品管理 / 商品SKU表（wms_sku）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_product, '商品SKU表', '/wms_warehouse/product/wms_sku/list', 'wms_sku/index', 'wms_warehouse_wms_sku:list', 2, 1, 1, 0);
SET @menu_wms_sku = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_sku, '新增', NULL, NULL, 'wms_warehouse_wms_sku:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_sku, '修改', NULL, NULL, 'wms_warehouse_wms_sku:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_sku, '删除', NULL, NULL, 'wms_warehouse_wms_sku:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_sku, 0);

-- wms 系统 / 其他 / 货主表（wms_owner）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_other, '货主表', '/wms_warehouse/other/wms_owner/list', 'wms_owner/index', 'wms_warehouse_wms_owner:list', 2, 1, 1, 0);
SET @menu_wms_owner = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_owner, '新增', NULL, NULL, 'wms_warehouse_wms_owner:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_owner, '修改', NULL, NULL, 'wms_warehouse_wms_owner:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_owner, '删除', NULL, NULL, 'wms_warehouse_wms_owner:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_owner, 0);

-- wms 系统 / 库存管理 / 库存表（wms_inventory）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '库存表', '/wms_warehouse/stock/wms_inventory/list', 'wms_inventory/index', 'wms_warehouse_wms_inventory:list', 2, 1, 1, 0);
SET @menu_wms_inventory = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory, '新增', NULL, NULL, 'wms_warehouse_wms_inventory:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory, '修改', NULL, NULL, 'wms_warehouse_wms_inventory:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory, '删除', NULL, NULL, 'wms_warehouse_wms_inventory:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_inventory, 0);

-- wms 系统 / 库存管理 / 库存事务流水表（wms_inventory_txn）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '库存事务流水表', '/wms_warehouse/stock/wms_inventory_txn/list', 'wms_inventory_txn/index', 'wms_warehouse_wms_inventory_txn:list', 2, 1, 1, 0);
SET @menu_wms_inventory_txn = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory_txn, '新增', NULL, NULL, 'wms_warehouse_wms_inventory_txn:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory_txn, '修改', NULL, NULL, 'wms_warehouse_wms_inventory_txn:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory_txn, '删除', NULL, NULL, 'wms_warehouse_wms_inventory_txn:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_inventory_txn, 0);

-- wms 系统 / 库存管理 / 库存锁定表（wms_inventory_lock）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '库存锁定表', '/wms_warehouse/stock/wms_inventory_lock/list', 'wms_inventory_lock/index', 'wms_warehouse_wms_inventory_lock:list', 2, 1, 1, 0);
SET @menu_wms_inventory_lock = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory_lock, '新增', NULL, NULL, 'wms_warehouse_wms_inventory_lock:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory_lock, '修改', NULL, NULL, 'wms_warehouse_wms_inventory_lock:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inventory_lock, '删除', NULL, NULL, 'wms_warehouse_wms_inventory_lock:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_inventory_lock, 0);

-- wms 系统 / 入库管理 / 入库单主表（wms_inbound_order）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_inbound, '入库单主表', '/wms_warehouse/inbound/wms_inbound_order/list', 'wms_inbound_order/index', 'wms_warehouse_wms_inbound_order:list', 2, 1, 1, 0);
SET @menu_wms_inbound_order = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inbound_order, '新增', NULL, NULL, 'wms_warehouse_wms_inbound_order:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inbound_order, '修改', NULL, NULL, 'wms_warehouse_wms_inbound_order:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inbound_order, '删除', NULL, NULL, 'wms_warehouse_wms_inbound_order:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_inbound_order, 0);

-- wms 系统 / 入库管理 / 入库单明细表（wms_inbound_order_item）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_inbound, '入库单明细表', '/wms_warehouse/inbound/wms_inbound_order_item/list', 'wms_inbound_order_item/index', 'wms_warehouse_wms_inbound_order_item:list', 2, 1, 1, 0);
SET @menu_wms_inbound_order_item = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inbound_order_item, '新增', NULL, NULL, 'wms_warehouse_wms_inbound_order_item:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inbound_order_item, '修改', NULL, NULL, 'wms_warehouse_wms_inbound_order_item:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_inbound_order_item, '删除', NULL, NULL, 'wms_warehouse_wms_inbound_order_item:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_inbound_order_item, 0);

-- wms 系统 / 入库管理 / 收货记录表（wms_receipt_record）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_inbound, '收货记录表', '/wms_warehouse/inbound/wms_receipt_record/list', 'wms_receipt_record/index', 'wms_warehouse_wms_receipt_record:list', 2, 1, 1, 0);
SET @menu_wms_receipt_record = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_receipt_record, '新增', NULL, NULL, 'wms_warehouse_wms_receipt_record:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_receipt_record, '修改', NULL, NULL, 'wms_warehouse_wms_receipt_record:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_receipt_record, '删除', NULL, NULL, 'wms_warehouse_wms_receipt_record:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_receipt_record, 0);

-- wms 系统 / 入库管理 / 上架任务表（wms_putaway_task）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_inbound, '上架任务表', '/wms_warehouse/inbound/wms_putaway_task/list', 'wms_putaway_task/index', 'wms_warehouse_wms_putaway_task:list', 2, 1, 1, 0);
SET @menu_wms_putaway_task = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_putaway_task, '新增', NULL, NULL, 'wms_warehouse_wms_putaway_task:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_putaway_task, '修改', NULL, NULL, 'wms_warehouse_wms_putaway_task:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_putaway_task, '删除', NULL, NULL, 'wms_warehouse_wms_putaway_task:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_putaway_task, 0);

-- wms 系统 / 出库管理 / 出库单主表（wms_outbound_order）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_outbound, '出库单主表', '/wms_warehouse/outbound/wms_outbound_order/list', 'wms_outbound_order/index', 'wms_warehouse_wms_outbound_order:list', 2, 1, 1, 0);
SET @menu_wms_outbound_order = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_outbound_order, '新增', NULL, NULL, 'wms_warehouse_wms_outbound_order:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_outbound_order, '修改', NULL, NULL, 'wms_warehouse_wms_outbound_order:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_outbound_order, '删除', NULL, NULL, 'wms_warehouse_wms_outbound_order:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_outbound_order, 0);

-- wms 系统 / 出库管理 / 出库单明细表（wms_outbound_order_item）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_outbound, '出库单明细表', '/wms_warehouse/outbound/wms_outbound_order_item/list', 'wms_outbound_order_item/index', 'wms_warehouse_wms_outbound_order_item:list', 2, 1, 1, 0);
SET @menu_wms_outbound_order_item = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_outbound_order_item, '新增', NULL, NULL, 'wms_warehouse_wms_outbound_order_item:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_outbound_order_item, '修改', NULL, NULL, 'wms_warehouse_wms_outbound_order_item:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_outbound_order_item, '删除', NULL, NULL, 'wms_warehouse_wms_outbound_order_item:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_outbound_order_item, 0);

-- wms 系统 / 库存管理 / 库存分配明细表（wms_allocation）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '库存分配明细表', '/wms_warehouse/stock/wms_allocation/list', 'wms_allocation/index', 'wms_warehouse_wms_allocation:list', 2, 1, 1, 0);
SET @menu_wms_allocation = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_allocation, '新增', NULL, NULL, 'wms_warehouse_wms_allocation:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_allocation, '修改', NULL, NULL, 'wms_warehouse_wms_allocation:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_allocation, '删除', NULL, NULL, 'wms_warehouse_wms_allocation:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_allocation, 0);

-- wms 系统 / 出库管理 / 拣货任务表（wms_pick_task）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_outbound, '拣货任务表', '/wms_warehouse/outbound/wms_pick_task/list', 'wms_pick_task/index', 'wms_warehouse_wms_pick_task:list', 2, 1, 1, 0);
SET @menu_wms_pick_task = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_pick_task, '新增', NULL, NULL, 'wms_warehouse_wms_pick_task:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_pick_task, '修改', NULL, NULL, 'wms_warehouse_wms_pick_task:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_pick_task, '删除', NULL, NULL, 'wms_warehouse_wms_pick_task:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_pick_task, 0);

-- wms 系统 / 其他 / 波次表（wms_wave）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_other, '波次表', '/wms_warehouse/other/wms_wave/list', 'wms_wave/index', 'wms_warehouse_wms_wave:list', 2, 1, 1, 0);
SET @menu_wms_wave = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_wave, '新增', NULL, NULL, 'wms_warehouse_wms_wave:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_wave, '修改', NULL, NULL, 'wms_warehouse_wms_wave:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_wave, '删除', NULL, NULL, 'wms_warehouse_wms_wave:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_wave, 0);

-- wms 系统 / 出库管理 / 发货单表（wms_shipment）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_outbound, '发货单表', '/wms_warehouse/outbound/wms_shipment/list', 'wms_shipment/index', 'wms_warehouse_wms_shipment:list', 2, 1, 1, 0);
SET @menu_wms_shipment = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_shipment, '新增', NULL, NULL, 'wms_warehouse_wms_shipment:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_shipment, '修改', NULL, NULL, 'wms_warehouse_wms_shipment:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_shipment, '删除', NULL, NULL, 'wms_warehouse_wms_shipment:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_shipment, 0);

-- wms 系统 / 库存管理 / 盘点单表（wms_stocktake_order）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '盘点单表', '/wms_warehouse/stock/wms_stocktake_order/list', 'wms_stocktake_order/index', 'wms_warehouse_wms_stocktake_order:list', 2, 1, 1, 0);
SET @menu_wms_stocktake_order = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_stocktake_order, '新增', NULL, NULL, 'wms_warehouse_wms_stocktake_order:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_stocktake_order, '修改', NULL, NULL, 'wms_warehouse_wms_stocktake_order:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_stocktake_order, '删除', NULL, NULL, 'wms_warehouse_wms_stocktake_order:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_stocktake_order, 0);

-- wms 系统 / 库存管理 / 盘点明细表（wms_stocktake_item）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '盘点明细表', '/wms_warehouse/stock/wms_stocktake_item/list', 'wms_stocktake_item/index', 'wms_warehouse_wms_stocktake_item:list', 2, 1, 1, 0);
SET @menu_wms_stocktake_item = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_stocktake_item, '新增', NULL, NULL, 'wms_warehouse_wms_stocktake_item:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_stocktake_item, '修改', NULL, NULL, 'wms_warehouse_wms_stocktake_item:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_stocktake_item, '删除', NULL, NULL, 'wms_warehouse_wms_stocktake_item:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_stocktake_item, 0);

-- wms 系统 / 库存管理 / 移库单表（wms_transfer_order）
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@dir_grp_stock, '移库单表', '/wms_warehouse/stock/wms_transfer_order/list', 'wms_transfer_order/index', 'wms_warehouse_wms_transfer_order:list', 2, 1, 1, 0);
SET @menu_wms_transfer_order = LAST_INSERT_ID();
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_transfer_order, '新增', NULL, NULL, 'wms_warehouse_wms_transfer_order:add', 3, 1, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_transfer_order, '修改', NULL, NULL, 'wms_warehouse_wms_transfer_order:edit', 3, 2, 1, 0);
INSERT INTO sys_menu (parent_id, name, path, component, perms, type, sort, status, deleted)
VALUES (@menu_wms_transfer_order, '删除', NULL, NULL, 'wms_warehouse_wms_transfer_order:remove', 3, 3, 1, 0);
INSERT INTO sys_role_menu (role_id, menu_id, deleted) VALUES (1, @menu_wms_transfer_order, 0);

COMMIT;
