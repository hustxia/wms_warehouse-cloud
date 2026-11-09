# WMS 仓储系统 V1.0 业务闭环 · 版本规划

> **状态：已交付（2026-11-07），本文作为历史基线保留。**
> **决策变更**：3.2 节「业务只写单体、cloud 保持骨架」的结论，已于 2026-11-07 按项目方向整体推翻——业务闭环最终迁入 `wsm_cloud/`，单体 `wsm_backend/` 于 2026-11-08 从仓库移除。后续版本与当前待办请看 [`WMS-版本路线图.md`](./WMS-版本路线图.md)。
>
> **编号说明**：仓储系统此前只有「表 + CRUD」，从未有过业务版本。本版是第一个带业务语义的版本，独立编号 V1.0。
> 与代码生成器的 V7.x、微服务骨架的 V8 不在同一编号体系——那两个号是"生成能力"和"架构拆分"的版本号，不是业务版本号。
> 如果你希望沿一条线往下数，改成 V9 也只需改本文件标题与 git tag。

---

## 一、现状盘点（2026-11-07 实测）

| 维度 | 实测结果 |
|---|---|
| 业务表 | **21 张**（仓库/库区/库位/货主/SKU/库存/流水/锁定/入库单+明细/收货/上架/出库单+明细/分配/拣货/波次/发运/盘点单+明细/调拨） |
| 后端接口 | **84 个** = 21 × 4，全部是 `POST /page`、`POST /export`、`GET /{id}`、`DELETE /{id}` |
| Service 实现 | **21 个 `*ServiceImpl`，每个 17 行**，`extends ServiceImpl<M, T>` 后只有一行注释占位 |
| Mapper XML | **0 个**（纯 `BaseMapper`，没有一条手写 SQL） |
| 业务事务 | **0 处**（唯一的 `@Transactional` 在基座 `BuiltinFlowService`） |
| 前端页面 | **21 个列表页共 12,059 行**，按钮清一色「新增 / 编辑 / 删除 / 详情 / 导出」 |
| 后端代码量 | 3,238 行 / 224 个 java 文件（≈ 14 行/文件，几乎全是声明） |

### 表结构却相当完整

这不是"设计没做好"的项目，恰恰相反，字段和约束都按真实 WMS 预留了：

- `wms_inventory`：批次号、生产日期、到期日期、`qty_on_hand`/`qty_allocated`/`qty_locked`、生成列 `qty_available`、乐观锁 `version`、唯一键 `uk_inv(warehouse, owner, sku, location, batch)`、`idx_expiry` 索引（FEFO 就位）
- `wms_inbound_order_item`：`expected_qty` / `received_qty` / `putaway_qty` + `PENDING/PARTIAL/DONE`
- `wms_outbound_order_item`：`order_qty` / `allocated_qty` / `picked_qty` / `shipped_qty` + `PENDING/PARTIAL/DONE`
- `wms_stocktake_item`：`system_qty` / `counted_qty` / 生成列 `diff_qty` / `counted_by` / `counted_at`
- 主表注释里已写明状态机：入库 `CREATED→IN_TRANSIT→RECEIVING→RECEIVED→PUTAWAY_DONE`，出库 `CREATED→ALLOCATED→PICKING→PICKED→SHIPPED`

## 二、结论：缺的不是设计，是「变更」

三个数字足以说明问题：

- 明细表里有 `received_qty`，但**没有任何代码会去加它**——只能靠人手在弹窗里填
- 库存表有 `qty_allocated`，但分配、拣货、发运之间**没有因果关系**，改一个不影响另一个
- 21 个页面能录入 21 种数据，但**一单从收货走到发运，要人工在 6 个页面各点一次"编辑"**

所以本版的目标不是加表、不是加页面，而是：**让数据之间产生因果**。一句话定义验收标准——**一张入库单收货上架后，库存自动变了；一张出库单发运后，库存自动减了、流水自动有了、数量字段自动自洽了。**

## 三、开工前必须先定的两个口径

### 3.1 可用量口径：现在的算法会超卖

```sql
qty_available DECIMAL(14,3) GENERATED ALWAYS AS (qty_on_hand - qty_allocated) STORED
```

**只减了 `allocated`，没减 `locked`。** 而 `wms_inventory_lock.lock_type` 的取值是 `PICK/TRANSFER/QC`——其中 `PICK` 与 `allocated` 的语义几乎重合，`TRANSFER`/`QC` 占用的量则完全不计入可用量。

后果：一个库位在库 100、盘点冻结 100（`qty_locked=100`），可用量仍显示 100，此时来一张出库单可以继续分配——**账面超卖**。

需要定口径并落到代码：

| 字段 | 语义（建议） | 谁写它 |
|---|---|---|
| `qty_allocated` | 出库单已分配、尚未发运的占用 | 分配 / 取消分配 / 发运 |
| `qty_locked` | 非出库场景的临时冻结 | 盘点冻结、质检冻结、调拨在途 |
| `qty_available` | `on_hand - allocated - locked` | 生成列，不由代码写 |

配套动作：
1. `lock_type` 枚举去掉 `PICK`，只保留 `STOCKTAKE / QC / TRANSFER`
2. 生成列不能就地改表达式，迁移脚本须 `ALTER TABLE ... DROP COLUMN qty_available` 再 `ADD COLUMN`（幂等写法套用 V7.4 的 `information_schema` + `PREPARE` 范式）

### 3.2 业务逻辑落在单体还是微服务

`wms_warehouse-cloud`（V8，7 个模块）与单体 **共用同一个 `wms_warehouse` 库**，且它的 5 个业务模块同样是 18 行空壳。

共库意味着**不能两边同时写业务**——同一张 `wms_inventory` 被两套代码改，锁和状态机会互相打架。

建议：**业务只写在单体**，微服务版保持"拆分骨架"的定位（它证明的是架构可拆，不是业务完整）。等单体业务闭环跑通、逻辑稳定后，再按领域把 Service 整段搬进 cloud 对应模块——那时是**搬运**，不是**重写**。

## 四、P0：四条闭环（缺一条就不算闭环）

### P0-1 库存变动唯一入口（地基，其他三条都依赖它）

**现状**：`WmsInventoryServiceImpl` 是空壳，没有任何代码能安全地改库存。

**要做**：新增 `InventoryCoreService`，所有库存变动只能走它：

| 方法 | 语义 | 原子性做法 |
|---|---|---|
| `increaseOnHand(inv, qty, txnType, refNo)` | 收货/上架/盘盈 | 存在则 `UPDATE ... qty_on_hand = qty_on_hand + ?`，不存在则 insert（走 `uk_inv` 唯一键兜底） |
| `decreaseOnHand(inv, qty, txnType, refNo)` | 发运/盘亏/调拨出 | `UPDATE ... WHERE id=? AND qty_on_hand >= ?`，影响行数 0 → 抛「库存不足」 |
| `allocate(inv, qty, refNo)` | 出库分配 | `UPDATE ... WHERE id=? AND qty_on_hand - qty_allocated - qty_locked >= ?` |
| `releaseAllocate(inv, qty)` | 取消分配 | 同上反向，`WHERE qty_allocated >= ?` |
| `lock / unlock(inv, qty, lockType)` | 盘点/质检/调拨冻结 | 同上 |

**硬规则**：
1. 每个方法**必须**在同一个事务里写一条 `wms_inventory_txn`（含 `qty_before` / `qty_after` / `ref_order_no`），没有流水就等于没发生
2. 扣减一律用「条件 UPDATE + 检查影响行数」，**不要**先 select 再 update（那是超卖的经典写法）
3. 乐观锁 `version` 保留，但作为第二道防线，不是主手段

**验收（二值）**：50 线程并发对同一库存记录各扣 1 件（初始 100 件）→ 成功恰好 100 次、`qty_on_hand = 0`、`wms_inventory_txn` 恰好 100 条、全程无负数、无死锁异常。

---

### P0-2 入库闭环：收货 → 上架

**要做**：

```
POST /wms_inbound_order/{id}/receive    body: [{itemId, receivedQty, batchNo, productionDate, expiryDate}]
POST /wms_inbound_order/{id}/putaway    body: [{itemId, locationId, putawayQty}]
```

**状态机**：`CREATED → IN_TRANSIT → RECEIVING（首次收货）→ RECEIVED（行全 DONE）| 保持 RECEIVING（部分）→ PUTAWAY_DONE（全部上架）`

**关键规则**：
- 收货可分批、可超收（超收超过 10% 需单独权限，本期先拒绝超收）
- 每次收货累加 `received_qty`，行状态 `0 < received < expected` → `PARTIAL`，`= expected` → `DONE`，`> expected` → 拒绝
- 上架按 `(warehouse, owner, sku, location, batch)` 定位库存记录，无则新建（批次/效期从明细带下来）
- 上架成功后库存 `+putawayQty`，流水类型 `PUTAWAY`，`ref_order_no` = 入库单号

**验收**：3 行明细、先收 2 行 → 单据 `RECEIVING`、2 行 `PARTIAL`；补齐收货 → `RECEIVED`；全部上架 → `PUTAWAY_DONE`，库存记录数 = 上架行数，`wms_inventory_txn` 条数 = 上架行数（逐条核对 `qty_before`/`qty_after` 连续）。

---

### P0-3 出库闭环：分配 → 拣货 → 发运

**要做**：

```
POST /wms_outbound_order/{id}/allocate   分配（可指定策略，默认 FEFO）
POST /wms_outbound_order/{id}/pick       拣货确认 body: [{itemId, pickedQty, locationId}]
POST /wms_outbound_order/{id}/ship       发运 body: {carrier, trackingNo}
```

**状态机**：`CREATED → ALLOCATED（或 PARTIAL）→ PICKING → PICKED → SHIPPED`

**关键规则**：
- **分配策略**：按 `expiry_date ASC`（NULL 排最后）、`batch_no ASC`、`location_id ASC` 依次占用；只选 `qty_on_hand - qty_allocated - qty_locked >= 需求量` 的库存记录
- 分配成功 → 写 `wms_allocation` 明细（记录每个库存记录占多少）+ `qty_allocated` 增加；**这一步不动 `on_hand`**
- 库存不足 → 能分多少分多少，`allocated_qty` 记实际值，行状态 `PARTIAL`，单据状态 `PARTIAL`，缺量 = `order_qty - allocated_qty`（不新增字段，用差值算）
- 拣货确认 → `picked_qty` 累加，同时 `qty_allocated -= pickedQty`、`qty_on_hand -= pickedQty`，流水 `PICK`
- 发运 → 写 `wms_shipment`，`shipped_qty` 累加，单据 `SHIPPED`
- **取消分配**：单据回 `CREATED`，`qty_allocated` 释放，`wms_allocation.status` 置 0

**验收**：库存在库 10（两个批次：A 批次效期近 6 件、B 批次效期远 4 件），出库单要 8 件 → 分配结果必须是 A 6 件 + B 2 件（FEFO 生效），单据 `ALLOCATED`；拣货 8 件后 `on_hand = 2`、`allocated = 0`、流水 2 条；发运后单据 `SHIPPED`。再测一单要 12 件 → 只分到 10 件、状态 `PARTIAL`、缺量 2。

---

### P0-4 盘点闭环：冻结 → 实盘 → 差异调整

**要做**：

```
POST /wms_stocktake_order            创建盘点单，按范围快照生成 item（system_qty = 当前 on_hand）
POST /wms_stocktake_order/{id}/freeze    冻结范围内库存（lock_type=STOCKTAKE）
POST /wms_stocktake_item/{id}/count      录实盘（counted_qty / counted_by / counted_at）
POST /wms_stocktake_order/{id}/confirm   确认差异并调整
```

**关键规则**：
- 创建时快照 `system_qty`，之后库存再变不影响本次盘点（差异以此快照为基准）
- 冻结期间该库存记录**不可被出库分配**（因为 `qty_locked` 让 available 归零）
- 确认时按 `diff_qty` 调整 `on_hand`，流水类型 `ADJUST`，正负号跟随差异
- 确认后释放冻结，`wms_inventory_lock.status = 0`、写 `released_at`
- 盘亏不能让 `on_hand` 变负——若已变负（盘点期间被发运），确认时拒绝并提示重新盘点

**验收**：账面 100、实盘 98 → 确认后 `on_hand = 98`、`diff_qty = -2`、流水 1 条 `ADJUST -2`、锁定量归零、该库位可用量恢复。再测盘盈（实盘 105）→ `on_hand = 105`、流水 `ADJUST +5`。

## 五、P1 / P2

**P1（闭环跑通后紧接着做）**
- **调拨**：同仓移库（一条流水出、一条流水入）+ 跨仓调拨（在途库存，用 `lock_type=TRANSFER`）
- **波次**：按承运商/区域合并多张出库单，生成一个波次和一组拣货任务
- **报表**：收发存汇总（按 SKU/仓库/时间段）、库龄分析、周转率、呆滞料
- **单据打印**：`printPage.ftl` 模板已在生成器里，接上数据即可出拣货单/入库单

**P2（有真实使用场景再做）**
- PDA / 移动端扫码（收货、上架、拣货），这是 WMS 落地的临界点，但没有真实硬件前做了也是空转
- 批次追溯链（正向：批次→入库单→供应商；反向：成品→领用批次）
- 多货主计费（仓储费/操作费/耗材费，按天或按件）
- 库位策略（上架推荐、ABC 分区、混放规则）

## 六、技术约定（四条，避免后期返工）

1. **状态机集中管理**：状态与允许的转移写成枚举表，非法转移直接抛业务异常。不要把 `if (status == "CREATED")` 散落在 Service 各处。
2. **库存变动只有一个门**：任何人绕过 `InventoryCoreService` 直接 `updateById` 改 `qty_*`，都会让流水和库存脱节——代码评审时按此判定。
3. **业务端点幂等**：`receive` / `pick` / `ship` / `confirm` 接收客户端幂等键（前端每次提交生成 UUID），重复提交返回上次结果而不是报错。
4. **前端只调业务端点，不再直接改数量字段**：`page`/`update` 接口保留给主数据维护，单据的数量字段在前端改为**只读**，由业务端点写入。否则人工改一次数量就把整条闭环破坏了。

## 七、整体验收：一条脚本跑完

一个可重复执行的 `scripts/e2e-wms.mjs`（参照代码生成器 `smoke-fresh-db.mjs` 的思路）：

1. 建 1 仓库 / 1 库区 / 2 库位 / 2 SKU（一个带效期、一个不带）/ 1 货主
2. 入库单 3 行 → 收货 → 上架 → 断言库存 = 上架量、流水条数、单据状态
3. 出库单 2 行 → 分配（断言 FEFO 选批）→ 拣货 → 发运 → 断言 `on_hand`/`allocated` 归零、流水条数
4. 盘点单 → 冻结 → 录差异 → 确认 → 断言 `on_hand` 调整值、锁定量归零
5. 并发场景：50 线程抢 100 件 → 断言不超卖
6. 全程结束后校验**恒等式**：每张库存记录 `on_hand = 该 SKU+库位+批次的所有流水 qty_change 之和`

第 6 条是最有价值的断言：它一次性证明"库存和流水没有脱节"，任何一处漏写流水都会在这里暴露。

## 八、风险与不做的事

**风险**
1. **改生成列口径要动已有数据**：`qty_available` 是 STORED 生成列，DDL 需要 DROP + ADD，重算期间该表会锁。当前是空库所以无痛，一旦有生产数据要选窗口期。
2. **`uk_inv` 不含 `deleted`**：逻辑删除后无法向同库位同批次再插入（唯一键冲突）。要么把 `deleted` 纳入唯一键，要么库存记录改物理删除。这是必须先修的隐性 bug，否则"收货→退货→再收货"第二次就失败。
3. **单体与微服务双写诱惑**：共库下若有人图省事在 cloud 里也改了库存，两套逻辑会冲突。建议在 cloud 的 README 里明确标注"业务逻辑以单体为准"。
4. **前端数量字段开放编辑**：目前 21 个页面都能直接改 `qty_*`。若不改成只读，闭环会被人工操作随时破坏（这也是技术约定第 4 条的原因）。

**本版明确不做**
- 不做移动端/PDA、不做 WMS 与 ERP/TMS 的外部对接、不做多仓多组织（现有 `warehouse_id` 已够单组织多仓）
- 不做自动补货/波次优化算法，先把"能跑通"做扎实

---

## 附：工作量估计（按 P0 拆）

| 任务 | 涉及文件 | 相对量 |
|---|---|---|
| P0-1 库存入口 + 流水 | 新增 1 个 Service + 1 个 Mapper 手写 SQL + 迁移脚本 | 中 |
| P0-2 入库闭环 | 2 端点 + 1 Service 实现 + 前端 1 个页面改造 | 中 |
| P0-3 出库闭环 | 3 端点 + FEFO 分配算法 + 前端 1 个页面改造 | **大**（分配逻辑是难点） |
| P0-4 盘点闭环 | 4 端点 + 差异调整 + 前端 1 个页面改造 | 中 |
| 整体验收脚本 | 1 个 mjs | 小 |

前端改造集中在 4 个页面（入库单、出库单、盘点单、库存），其余 17 个主数据页面不动。
