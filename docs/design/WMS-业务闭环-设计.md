# WMS 业务闭环 设计

> 关联需求：`docs/requirements/WMS-业务闭环-需求.md`
> 关联规划：`docs/WMS-V1.0-业务闭环-规划.md`
> 版本：v1.0　日期：2026-11-07

## 一、表结构

### 1.1 现有表（沿用，不重建）

21 张业务表已在 `docs/../db/init_tables.sql` 中定义并建库，全部沿用。本版只改一处、加一张。

### 1.2 变更一：`wms_inventory.qty_available` 改口径

**问题**：旧表达式 `qty_on_hand - qty_allocated` 漏减 `qty_locked`，盘点冻结期间可用量不变 → 出库单仍能分配 → 超卖。

**变更**（`docs/sql/V1.0__wms_inventory_core.sql`，幂等，已执行两遍）：

```sql
-- 旧
qty_available DECIMAL(14,3) GENERATED ALWAYS AS (qty_on_hand - qty_allocated) STORED

-- 新
qty_available DECIMAL(14,3) GENERATED ALWAYS AS (qty_on_hand - qty_allocated - qty_locked) STORED
```

MySQL 不支持就地改生成列表达式，只能 `DROP COLUMN` + `ADD COLUMN`（重建表）。执行前表内 **0 行**，无数据风险；已先 `mysqldump` 全库留底（`.backup/wms_warehouse-before-v1.0.sql`，78K）。

**执行结果**（`information_schema`）：

```
COLUMN_NAME     COLUMN_TYPE      GENERATION_EXPRESSION
qty_available   decimal(14,3)    ((`qty_on_hand` - `qty_allocated`) - `qty_locked`)
```

### 1.3 变更二：新增幂等键表

```sql
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
```

**为什么不用「查单据状态」做幂等**：状态机只能挡住「已发运再发运」，挡不住「同一状态下的重复提交」——例如连点两次拣货，两次都过状态校验，库存被扣两遍。必须有独立的幂等键。

### 1.4 语义约定（不落 DDL）

`wms_inventory_lock.lock_type` 取值收敛为 `STOCKTAKE` / `QC` / `TRANSFER`，**去掉 `PICK`**。

理由：`PICK` 的占用语义已由 `wms_inventory.qty_allocated` 表达，若再往 `qty_locked` 里记一次，可用量会被扣两遍。

### 1.5 `uk_inv` 与逻辑删除的冲突：不改表，改写法

**问题**：`UNIQUE KEY uk_inv (warehouse_id, owner_id, sku_id, location_id, batch_no)` 不含 `deleted`，而逻辑删除全局开启（`logic-delete-field: deleted` + `BaseEntity` 的 `@TableLogic`）。库存记录软删后行仍在表里占着 key，再收货到同库位同批次 → `Duplicate entry`。

**为什么不把 `deleted` 加进唯一键**：坑库 `dev-doc/02-坑/mysql.md` 已有实证（mall 项目 2026-10-31）——`deleted` 只有 0/1 两个值，等于「同一业务键最多允许一条已删行」，**第二次软删照样撞**。这是结构性冲突，加列绕不开。

**本版解法**：不改表结构，用 `INSERT ... ON DUPLICATE KEY UPDATE` 让已删行「复活」：

```sql
INSERT INTO wms_inventory
  (warehouse_id, owner_id, sku_id, location_id, batch_no, production_date, expiry_date, qty_on_hand, qty_allocated, qty_locked, version, deleted)
VALUES (#{warehouseId}, #{ownerId}, #{skuId}, #{locationId}, #{batchNo}, #{productionDate}, #{expiryDate}, #{qty}, 0, 0, 0, 0)
ON DUPLICATE KEY UPDATE
  qty_on_hand = qty_on_hand + VALUES(qty_on_hand),
  deleted     = 0
```

业务上也更合理：同一库位同一批次本来就该只有一条库存记录，软删后重新收货应该复用这条，而不是插一条新的。

### 1.6 种子数据

`docs/sql/V1.0__wms_seed.sql`（固定 id 1001+，`ON DUPLICATE KEY UPDATE` 幂等，与既有 id 1~8 互不干扰）。

场景专为 FEFO 验收设计：

| 表 | 数据 |
|---|---|
| wms_warehouse | 1001 `WH-DEMO` 演示仓 |
| wms_zone | 1001 `Z-RCV` 收货区、1002 `Z-STO` 存储区 |
| wms_location | 1001 `A-01-01`、1002 `A-01-02`（存储）、1003 `R-01-01`（收货） |
| wms_owner | 1001 `OW-DEMO` |
| wms_sku | 1001 `SKU-1001`（批次管理、保质期 180 天）、1002 `SKU-1002`（不管批次） |
| **wms_inventory** | 1001 库位 A-01-01：批次 `B20261201` 效期 2026-12-01 **6 件**；批次 `B20270601` 效期 2027-06-01 **4 件**；1003：`SKU-1002` 50 件 |
| wms_inbound_order | 1001 `IB-DEMO-0001`，`CREATED`，3 行明细（20 / 10 / 30 件） |
| wms_outbound_order | 1001 `OB-DEMO-0001`，`CREATED`，1 行要 8 件 |

**执行结果**：

```
t              c
warehouse      1
location       3
sku            2
inventory      3
inbound_item   3
outbound_item  1
```

## 二、功能设计

| 编号 | 功能点 | 页面/位置 | 字段与操作 | 状态流转 | 验收标准 |
|---|---|---|---|---|---|
| F-01 | 库存变动统一入口 | 后端 `InventoryCoreService` | `increase/decrease/allocate/releaseAllocate/lock/unlock` 六个方法，禁止外部直改 `qty_*` | — | 全项目 `setQtyOnHand/setQtyAllocated/setQtyLocked` 除本类外无命中 |
| F-02 | 流水自动记录 | 同上 | 每次变动同事务插 `wms_inventory_txn`，必带 `qty_before`/`qty_after`/`ref_order_no` | — | `qty_before + qty_change = qty_after` 恒成立 |
| F-03 | 可用量口径 | 数据库 | 生成列已改，代码不写该字段 | — | 冻结 100 后可用量 0 |
| F-04 | 软删记录复活 | `InventoryMapper.increaseOnHand` | `ON DUPLICATE KEY UPDATE ... deleted=0` | — | 软删后收货同批次，行数不增、`deleted=0` |
| F-05 | 收货登记 | 入库单页 → 「收货」弹窗 | 每行填实收数量；累加 `received_qty` | 行 `PENDING→PARTIAL→DONE` | 3 行收 2 行 → 单据 `RECEIVING` |
| F-06 | 上架 | 入库单页 → 「上架」弹窗 | 每行填上架数量+目标库位 | 单据 `RECEIVED→PUTAWAY_DONE` | 库存记录 +N 条，流水 N 条 `PUTAWAY` |
| F-07 | 入库单状态流转 | 后端状态机 | 非法流转抛业务异常 | `CREATED→IN_TRANSIT→RECEIVING→RECEIVED→PUTAWAY_DONE` | 跳过收货直接上架 → 拒绝 |
| F-08 | 库存分配 FEFO | 出库单页 → 「分配」 | 一次点击按单分配全部行 | 单据 `CREATED→ALLOCATED/PARTIAL` | 在库 10（6 近效期+4 远效期）要 8 → 分 6+2 |
| F-09 | 拣货确认 | 出库单页 → 「拣货」弹窗 | 每行填实拣数量 | 单据 `→PICKING→PICKED` | `on_hand` 减、`allocated` 减、流水 `PICK` |
| F-10 | 发运 | 出库单页 → 「发运」弹窗 | 填承运商+运单号 | 单据 `→SHIPPED` | 写 `wms_shipment`，`shipped_qty` 累加 |
| F-11 | 取消分配 | 出库单页 → 「取消分配」 | 二次确认 | 单据回 `CREATED` | `allocated` 归 0，allocation 置失效 |
| F-12 | 出库单状态流转 | 后端状态机 | 非法流转拒绝 | `CREATED→ALLOCATED/PARTIAL→PICKING→PICKED→SHIPPED` | 未拣完就发运 → 拒绝 |
| F-13 | 创建盘点单 | 盘点单页 → 「新建盘点」 | 选仓库/库区/库位范围 | `CREATED` | item 的 `system_qty` = 当前 `on_hand` |
| F-14 | 冻结 | 盘点单页 → 「冻结」 | 按 item 逐条加锁 | `CREATED→FROZEN` | 冻结期间该库位可用量 0 |
| F-15 | 录实盘 | 盘点明细行内编辑 | 填 `counted_qty` | item `PENDING→COUNTED` | `diff_qty` 自动算出 -2 |
| F-16 | 确认调整 | 盘点单页 → 「确认调整」 | 二次确认 | `FROZEN→CONFIRMED` | `on_hand` 变为实盘值，流水 `ADJUST`，锁释放 |
| F-17 | 入库单页业务化 | `views/wms_inbound_order` | 加「收货」「上架」按钮；`received_qty`/`putaway_qty` 列只读；状态列中文映射 | — | 按钮按状态禁用 |
| F-18 | 出库单页业务化 | `views/wms_outbound_order` | 加「分配」「拣货」「发运」「取消分配」；数量列只读 | — | 同上 |
| F-19 | 盘点单页业务化 | `views/wms_stocktake_order` | 加「冻结」「确认调整」；明细页可录实盘 | — | 差异列红绿高亮 |
| F-20 | 库存页只读化 | `views/wms_inventory` | `qty_*` 只读，去掉新增/删除 | — | 页面无「新增」「删除」按钮 |

## 三、接口设计

**约定**（沿用项目现状，不引入新风格）：
- 前缀无 `/api`，直接挂现有 controller 路径（下划线风格）；前端 dev 代理 `/dev-api` 转发时去前缀
- 统一返回 `{code, msg, data}`；**错误也是 HTTP 200 + body.code**（`40400` 不存在、`40000` 参数/业务校验失败）
- `Long` 主键由 `LongIdJacksonConfig` 转字符串下发
- 业务端点一律 `POST`，body 带 `requestId`（幂等键，前端 UUID）

| 接口 | 方法 | 路径 | 请求体 | 响应 data |
|---|---|---|---|---|
| 收货登记 | POST | `/wms_inbound_order/{id}/receive` | `{requestId, items:[{itemId, receivedQty}]}` | `{orderNo, status, items:[{itemId, receivedQty, status}]}` |
| 上架 | POST | `/wms_inbound_order/{id}/putaway` | `{requestId, items:[{itemId, locationId, putawayQty}]}` | `{orderNo, status, putawayQty}` |
| 分配 | POST | `/wms_outbound_order/{id}/allocate` | `{requestId}` | `{orderNo, status, items:[{itemId, allocatedQty, allocations:[{inventoryId, batchNo, locationId, qty}]}]}` |
| 拣货确认 | POST | `/wms_outbound_order/{id}/pick` | `{requestId, items:[{itemId, pickedQty}]}` | `{orderNo, status, pickedQty}` |
| 发运 | POST | `/wms_outbound_order/{id}/ship` | `{requestId, carrier, trackingNo}` | `{orderNo, status, shipmentNo}` |
| 取消分配 | POST | `/wms_outbound_order/{id}/cancel-allocate` | `{requestId}` | `{orderNo, status}` |
| 创建盘点单 | POST | `/wms_stocktake_order` | `{warehouseId, zoneId?, locationId?, stocktakeType}` | `{stocktakeNo, itemCount}` |
| 冻结 | POST | `/wms_stocktake_order/{id}/freeze` | `{requestId}` | `{stocktakeNo, status, lockedCount}` |
| 录实盘 | POST | `/wms_stocktake_item/{id}/count` | `{requestId, countedQty}` | `{itemId, systemQty, countedQty, diffQty, status}` |
| 确认调整 | POST | `/wms_stocktake_order/{id}/confirm` | `{requestId}` | `{stocktakeNo, status, adjustedCount, diffTotal}` |

**错误码对照**

| 场景 | HTTP | body.code | msg |
|---|---|---|---|
| 单据不存在 | 200 | 40400 | 入库单不存在 / 出库单不存在 / 盘点单不存在 |
| 状态不允许 | 200 | 40000 | 当前状态「X」不允许此操作 |
| 数量校验失败 | 200 | 40000 | 收货数量必须大于 0 / 上架数量不能大于已收数量 |
| 库存不足 | 200 | 40000 | 无可用库存（分配时**不报错**，走部分分配） |
| 未登录 | 200 | 401 | 未登录或登录已过期 |

## 四、验证记录

> S8 阶段填写，须粘贴真实命令输出。
