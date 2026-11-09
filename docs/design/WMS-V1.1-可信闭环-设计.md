# WMS-V1.1 可信闭环 设计

> 关联需求：`docs/requirements/WMS-V1.1-可信闭环-需求.md`
> 版本：v1.1　日期：2026-11-08
> 关联基线：`docs/design/WMS-业务闭环-设计.md`（V1.0，原文保留）

## 一、表结构

**本版不新增表、不改已有列、不执行 DDL。** 依据需求文档「待确认」：冲销复用 `wms_inventory_txn` 写 `ADJUST` 流水，对账只读现有表，本地消息表 / 事务消息归 V2.0。这样避开改表结构的破坏性操作，也让 `db-init.sh` 能完整重建当前库。

### 复用的表

| 表 | 用途 | 本版是否写入 |
|---|---|---|
| `wms_inventory` | 库存主记录，并发验收的扣减目标 | 并发验收 / 冲销写入（均经 `InventoryCoreService`） |
| `wms_inventory_txn` | 流水，`on_hand = Σ qty_change` 恒等式的依据 | 冲销写一条 `ADJUST` |
| `wms_request_log` | 冲销端点幂等键 | 冲销首次执行写一条 |
| `wms_inbound_order_item` / `wms_outbound_order_item` / `wms_stocktake_item` | 明细按单查询的数据源 | 只读 |
| `sys_session` | e2e 注入 admin 会话 | e2e 写（沿用 V1.0 同构做法） |

### 种子数据

沿用 `docs/sql/V1.0__wms_seed.sql` 的固定 id（1001 / 1002 / 1003，共 3 行库存）。并发验收 step 额外插入一条 **id=91001、`qty_on_hand=100`、批次 `CONCURRENT-TEST`** 的临时库存，跑完由 `reset()` 删除，不污染 V1.0 seed；`--keep` 模式下保留供人工查库。

### 实测（S2 门禁，2026-11-08 真实输出）

```
$ "C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -proot --default-character-set=utf8mb4 --database=wms_warehouse -B -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wms_warehouse';"
34

$ ... -e "SELECT 'wms_inventory',COUNT(*) ... UNION ALL ..."
wms_inventory	3
wms_inventory_txn	10
wms_request_log	1
wms_inbound_order_item	11
wms_outbound_order_item	9
wms_stocktake_item	0
sys_dict_data	9

$ ... -e "SELECT GENERATION_EXPRESSION FROM information_schema.columns WHERE ... column_name='qty_available';"
((`qty_on_hand` - `qty_allocated`) - `qty_locked`)

$ ... -e "SELECT id,batch_no,qty_on_hand,qty_allocated,qty_locked FROM wms_inventory WHERE id IN (1001,1002,1003);"
1001	B20261201	6.000	6.000	0.000
1002	B20270601	4.000	2.000	0.000
1003		50.000	0.000	0.000
```

生成列口径已含 `qty_locked`，说明 `V1.0__wms_inventory_core.sql` 已在运行库执行过；`db-init.sh` 必须保持这个执行顺序。

**变更历史**：无（本版零 DDL）。

## 二、功能设计

| 编号 | 功能点 | 页面 / 位置 | 字段与操作 | 状态流转 | 验收标准 |
|---|---|---|---|---|---|
| F-01 | 建库脚本归位 | `wsm_cloud/db/` | 5 个 `init_*.sql` 已从 `fa1fa24` 恢复 | — | `git ls-files wsm_cloud/db \| wc -l` = 6 |
| F-02 | 前端只读 | 4 个业务页弹窗控件行 | `status` / `qtyOnHand` / `qtyAllocated` / `qtyLocked` / `qtyAvailable` / `version` / `carrier` / `trackingNo` / `confirmedAt` 置 `disabled`，placeholder「由业务端点写入」；同步删对应 `rules.required` | — | 控件不可输入，且新增一条主数据能提交 |
| F-03 | 后端白名单 | `wms-common` 全局 `RequestBodyAdvice` | 精确匹配 `POST/PUT /wms_{inbound_order,outbound_order,stocktake_order,inventory}` 根路径；命中受保护字段抛 `BusinessException` | — | 带 `qtyOnHand` 的 PUT 被拒且库值不变；去掉该字段放行 |
| F-04 | 操作收敛 | 库存页 / 盘点页 | 库存页删「新增」「删除」按钮 + import + `handleDelete`；盘点页隐藏生成器「新增盘点单表」，只留业务端点 | — | DOM 无新增/删除按钮，盘点页一个创建入口 |
| F-05 | 并发验收 | `scripts/e2e-cloud.mjs` 新增 step | 复用 `api()` / `raw()` / `sql()` / `step()`；`Promise.all` 100 个并发 `decrease`，再 150 个 | — | 100/100 成功、`on_hand=0`、流水 100、无负数；150 时拒绝 50 |
| F-06 | 明细按单查询 | 3 个明细 Controller 手写区 + `src/api/wms_biz.js` | `LambdaQueryWrapper.eq(::getOrderId / ::getStocktakeId, id)`；前端去掉 `fetchAll` 拉明细 | — | 250 行明细弹窗完整显示 |
| F-07 | 一键建库 | `scripts/db-init.sh` | 建库 → utf8mb4 → 6 个 `db/*.sql` → 2 个 `V1.0__*.sql` | — | 空库可跑、重复跑不报错 |
| F-08 | 对账与冲销 | `scripts/reconcile.mjs` + `wms-inventory` 内部端点 | 比对 `on_hand` 与**该库存最近一条流水的 `qty_after`**（增量口径，见下方修正）；冲销写 `ADJUST` 流水 | — | 造差异 → 报 1 条 → 冲销后 0 条 |

> **S6 口径修正（2026-11-08 实测）**：原设计写的是「`on_hand = Σ 全量流水 qty_change`」，但 seed 库存自带期初量却无期初流水——实测干净库上 `1001/1002/1003` 分别报 `6/4/50` 三条**假差异**，恒等式根本不成立。改为增量口径：只比对「当前账面 vs 最近一条流水的 `qty_after`」，这恰好能捕获「绕过核心服务直接改库」或「跨服务远程成功、本地失败」造成的账面偏移。冲销写一条 `qty_change = target - before` 的 ADJUST 流水，既满足「前后台量差 = qty_change」，也让最新流水重新与账面吻合。
| F-09 | 安全默认值 | 5 个 `application.yml` + 5 处 `@Value` + `cloud-up.sh` | 去掉 `:root` / `:wms-internal`；脚本显式 `export DB_PASSWORD` / `APP_INTERNAL_TOKEN` | — | 裸 `java -jar` 失败；`cloud-up.sh` 仍能起全套 |
| F-10 | 门禁化 | `scripts/verify.sh` | 编译 → 库体检 → e2e → 对账 → 前端 build → 报告 | — | 全绿；故意改错断言则非零退出 |

### 页面结构

**4 个业务页弹窗（`el-form-item` → 控件行）**

| 页面 | 禁用（必须） | 建议禁用 | 保留可编辑 |
|---|---|---|---|
| 入库单 `wms_inbound_order/index.vue` | `status`(108) | `operatorId`/`createdAt`/`updatedAt` | `orderNo`/`orderType`/`warehouseId`/`ownerId`/`supplierId`/`refNo`/`expectedDate` |
| 出库单 `wms_outbound_order/index.vue` | `status`(109) | `carrier`(119)/`trackingNo`(124)/`createdAt`/`updatedAt` | `orderNo`/`orderType`/`warehouseId`/`ownerId`/`customerId`/`refNo`/`priority` |
| 盘点单 `wms_stocktake_order/index.vue` | `status`(99) | `createdBy`/`createdAt`/`confirmedAt`(114) | `stocktakeNo`/`warehouseId`/`zoneId`/`stocktakeType` |
| 库存 `wms_inventory/index.vue` | `qtyOnHand`(117)/`qtyAllocated`(122)/`qtyLocked`(127)/`qtyAvailable`(132) | `version`(137)/`updatedAt` | `warehouseId`/`ownerId`/`skuId`/`locationId`/`batchNo`/`productionDate`/`expiryDate` |

> 实现要点（来自前端调研）：`el-form-item` 自身不支持 `disabled`，必须加在内部控件标签上；被禁字段多数带 `rules.required`，不同步删除会让新增弹窗 `validate()` 直接失败。`src/components/biz/` 里「本次收货量/拣货量/实盘量」输入框是业务端点合法入参，**不禁用**。

**操作列**：库存页移除「新增库存表」(23) 与「删除」(58)，保留「详情」；盘点页保留 `<biz-stocktake-actions mode="toolbar">` 的创建入口，隐藏生成器新增按钮 (23)。

**并发验收结果面板**：挂在库存页 `BizInventoryPanel.vue` 的「库存恒等式」提示下方，展示最近一次 e2e 并发段的成功数/拒绝数/流水数/是否出现负数；数据来源为 `reconcile.mjs` 与 e2e 输出，页面不自行推导。

### 效果图

- [`ui/WMS-V1.1-业务字段只读.html`](ui/WMS-V1.1-业务字段只读.html)
- [`ui/WMS-V1.1-并发验收结果.html`](ui/WMS-V1.1-并发验收结果.html)
- [`ui/WMS-V1.1-对账与冲销.html`](ui/WMS-V1.1-对账与冲销.html)
- [`ui/WMS-V1.1-建库与门禁.html`](ui/WMS-V1.1-建库与门禁.html)

**确认状态**：已确认（用户 2026-11-08「开始开发」，S4 门禁放行，随即进入 S6/S7）。

## 三、接口设计

> 本节为 S5 产物，与 §2 同批产出；S6 之前不改代码。

| 接口 | 方法 | 路径 | 请求体 | 响应 |
|---|---|---|---|---|
| 通用 CRUD 字段拦截 | POST / PUT | `/wms_inbound_order`、`/wms_outbound_order`、`/wms_stocktake_order`、`/wms_inventory`（网关前缀剥离后） | `XxxSaveDTO`，含受保护字段 | HTTP 200 + `{"code":40001,"msg":"数量/状态字段由业务端点写入，禁止直接修改"}`；未含则原逻辑 `Result.ok()` |
| 入库明细按单查询 | GET | `/wms_inbound_order_item/by-order/{orderId}` | — | `Result<List<WmsInboundOrderItem>>` |
| 出库明细按单查询 | GET | `/wms_outbound_order_item/by-order/{orderId}` | — | `Result<List<WmsOutboundOrderItem>>` |
| 盘点明细按单查询 | GET | `/wms_stocktake_item/by-stocktake/{stocktakeId}` | — | `Result<List<WmsStocktakeItem>>` |
| 库存冲销（内部） | POST | `/internal/inventory/reconcile-adjust`（直连 8145，网关不路由） | `{inventoryId, targetOnHand, requestId, refNo?, remark?}`；`X-Internal-Token` 必带 | `Result<Map<String,Object>>`：`{inventoryId, txnId, before, after, qtyChange, replayed}`，ID 均为字符串 |
| 并发扣减探针 | POST | `/internal/inventory/decrease`（现有，复用） | `{inventoryId, qty:1, txnType:"PICK", refNo:"CONCURRENT-<n>", remark}` + `X-Internal-Token` | `Result<Void>`；库存不足时业务码非 200 |
| 对账脚本 | — | `scripts/reconcile.mjs`（直连 MySQL，非 HTTP） | `--json` 可选 | stdout 差异清单；有差异时退出码 1 |

### 字段类型约定

- 主键 `Long` → 序列化为**字符串**下发（雪花 ID 防 JS 精度丢失）；新增端点的 `Map` 返回值里尤其要 `String.valueOf(...)`。
- 数量 `DECIMAL(14,3)` → JSON number，后端 `BigDecimal.compareTo` 比较，禁止用 `==`。
- 时间 `yyyy-MM-dd HH:mm:ss`；状态枚举前端用 `statusLabel/statusTagType` 映射，不允许显示英文原文。
- 错误统一 HTTP 200 + `body.code`；前端 `request.js` 按 `code !== 0 && code !== 200` 弹提示。
- 新增内部端点必须与 `InternalCallConfig` 的 Feign 签名一致：本版冲销端点由运维脚本直连，返回 `Result<T>`，不新增 Feign 契约。
- 网关路由：`/wms_*_item/**` 已在 `wms-gateway/application.yml` 放行，新增子路径无需改路由；`/internal/**` 保持不路由。

## 四、验证记录（S8，2026-11-08 真实输出）

> 以下全部为实跑输出，未预填、未改写结论。环境：Nacos 8848 + 6 服务 + 前端 9040 全套在跑。

### 1. 编译

```
$ /d/tool/apache-maven-3.6.3/bin/mvn -o package -DskipTests
Reactor Summary:
  wms_warehouse-cloud .. SUCCESS [  0.005 s]
  wms-common ........... SUCCESS [ 13.024 s]
  wms-system ........... SUCCESS [  2.859 s]
  wms-master ........... SUCCESS [  7.535 s]
  wms-inbound .......... SUCCESS [  6.436 s]
  wms-outbound ......... SUCCESS [  6.476 s]
  wms-inventory ........ SUCCESS [  6.409 s]
  wms-gateway .......... SUCCESS [  0.646 s]
BUILD SUCCESS | Total time: 44.165 s
```

> 本机 `mvn` 不在 PATH，`scripts/verify.sh` 已加入绝对路径探测。

### 2. 接口（curl 实测）

| 场景 | 请求 | 期望 | 实际 |
|---|---|---|---|
| 后端字段白名单命中 | `PUT /wms_inventory` 带 `qtyOnHand=999` | 非 200 | `{"code":40001,"msg":"字段 qtyOnHand 由业务端点写入，禁止通过通用接口直接修改"}`；查库 `50.000` 不变 |
| 去掉受保护字段 | `PUT /wms_inventory` id=1001 不带 `qtyOnHand` | 200 | `{"code":200,"msg":"success"}` |
| 单据状态只读 | `POST /wms_inbound_order` 带 `status` | 非 200 | `{"code":40001,...}` |
| 主数据不误伤 | `POST /wms_warehouse` 带 `status=1` | 200 | `{"code":200}`（新增主数据提交成功，`rules.required` 未误伤） |
| 明细按单查询 | `GET /wms_inbound_order_item/by-order/1001` 等 3 个 | 200 + 全量 | 3 个端点均 `code:200`，主键序列化为字符串 |
| 冲销幂等 | 重复同一 `requestId` | 200 + `replayed=true` | `SELFTEST PASS`，ADJUST 流水恰好 1 条 |
| 安全默认值 | 不设 `DB_PASSWORD`/`APP_INTERNAL_TOKEN` 裸跑 `java -jar` | 启动失败 | `exit=1`；`Could not resolve placeholder 'app.internal.token'` |

> 时间字段必须用 `yyyy-MM-dd HH:mm:ss`：测试里误用 ISO `2026-11-08T12:00:00` 会触发
> `InvalidFormatException` → 500，改用项目格式后 200。

### 3. UI

```
$ NODE_OPTIONS= npm run build
 DONE  Build complete. The dist directory is ready to be deployed.

$ TOKEN=<admin 会话> node scripts/ui-check-pages.mjs
  PASS  入库单  表格 18 行 / 只读控件 4 个
  PASS  出库单  表格 18 行 / 只读控件 5 个
  PASS  盘点单  表格 16 行 / 只读控件 4 个 / 无「新增盘点单表」入口
  PASS  库存    表格  6 行 / 只读控件 6 个 / 无「新增库存表」、无「删除」按钮
结果：4/4 页面通过（console error 0 / JS 异常 0 / HTTP>=400 0 / 失败请求 0）
```

截图：`docs/evidence/v1.1/{入库单,出库单,盘点单,库存}.png`。
并发验收结果面板读的是 `public/wms-verify-report.json`（由 e2e 写出），页面不自行推导。

### 4. 端到端与门禁

```
$ node scripts/e2e-cloud.mjs
  100 并发 → 成功 100 / 拒绝 0 / 流水 100 / 无负数
  150 并发 → 成功 100 / 拒绝 50 / 流水 100 / 无负数
结果：134/134 断言通过（V1.0 的 118 条全部保持通过）

$ node scripts/reconcile.mjs --selftest
  造差异 57 vs 50 → 报 1 条 → 冲销写 ADJUST → 幂等回放 → 复查 0 条 → 现场恢复
$ node scripts/reconcile.mjs
对账通过：0 条差异

$ DB_NAME=wms_v11_check bash scripts/db-init.sh   # 空库第一次 rc=0
  表 34 / qty_available = ((qty_on_hand - qty_allocated) - qty_locked) / 字典 9 / 菜单 91 / 库存 3
$ DB_NAME=wms_v11_check bash scripts/db-init.sh   # 重复跑 rc=0，菜单仍 91 条

$ bash scripts/verify.sh --skip-build --with-ui --negative-test
  通过环节 9 / 耗时 93s / VERIFY PASS (rc=0)
  负向验证：故意把并发断言改成 99 → 脚本非零退出，门禁有效
```

### 5. 遗留问题

| 问题 | 影响 | 处理 |
|---|---|---|
| 跨服务「远程成功 + 本地失败」仍可能多出库存 | 本版只能对账发现 + 冲销修正，不能自动兜底 | V2.0 事务消息 / 本地消息表 |
| e2e 绕过图形验证码（插 `sys_session`） | 登录链路本身未被自动化覆盖 | 保持 V1.0 同构做法，另立议题 |
| `db/init_seed.sql` 写生成列且非幂等 | 直接执行会报 3105 并污染库存表 | `db-init.sh` 默认跳过；`--with-legacy-seed` 可选 + `--force` |
| 库存页「编辑」仍可改业务键（仓库/SKU/批次） | 本版只锁数量与状态，业务键可被人工改 | V1.2 评估：库存业务键是否需要同样只读 |
| `SaveDTO` 的系统字段校验被放松（生成区） | 重新生成会带回 `@NotNull/@NotBlank`，只读规则会再次失效 | 生成器模板需同步；已写入坑库与 S9 沉淀 |

## 修订记录

| 日期 | 版本 | 变更点 |
|---|---|---|
| 2026-11-08 | v1.1 | 初稿：S2 实测数据、S3 功能设计、S5 接口设计；S4 效果图待确认 |
| 2026-11-08 | v1.1 | S4 用户确认「开始开发」；S6 修正对账口径为增量（最近流水 `qty_after`）、冲销返回体补全、明确凭据注入方式 |
| 2026-11-08 | v1.1 | S8 三层验证证据落盘：编译 8/8、接口 7 项、UI 4/4、e2e 134/134、门禁 9 环节全绿 |
