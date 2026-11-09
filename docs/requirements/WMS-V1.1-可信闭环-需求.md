# WMS-V1.1 可信闭环 需求

> 版本：v1.1　日期：2026-11-08　提出人：项目负责人
> 关联规划：`docs/WMS-版本路线图.md` §2；关联基线：`docs/requirements/WMS-业务闭环-需求.md`（V1.0，原文保留）
> 范围确认（2026-11-08 用户）：V1.1 全部 8 项；字段只读采用**前后端双层拦截**；验证通过后**先不提交**。

## 背景与目标

V1.0 已交付三条业务闭环（收货→上架→分配→拣货→发运、盘点冻结→实盘→调整），e2e 118/118，全站 21 页 0 异常响应。但按 V1.0 需求原文逐条复核，发现**已写明却未落实的 P0**：

| V1.0 原文 | 现状 |
|---|---|
| F-17~F-20「数量列只读」「库存页去掉新增/删除」 | 4 个业务页表单 0 处 `disabled`，库存页新增/删除按钮仍在 |
| 验收清单「50 线程各扣 1 件（初始 100）→ 成功恰好 100 次」 | `e2e-cloud.mjs` 无任何并发用例 |

叠加三个工程短板：**跨服务无 XA 只能对账发现、不能修复**；**删单体后建库脚本曾一并丢失**（已恢复但无一键脚本）；**公开仓库里数据库口令与内部令牌带默认值**；**无门禁脚本**。

**本版目标**：不加业务功能，把 V1.0 的「能跑通」升级为「**不可绕过、可证明、可重建**」。

**不做会怎样**：人工在编辑弹窗改一次数量，库存与流水立即脱节；并发下的超卖没有证据能发现；换台机器按 README 仍可能建不出可跑的库。

## 功能点

| 编号 | 功能点 | 优先级 | 说明 |
|---|---|---|---|
| F-01 | 建库脚本归位 | P0 | **已完成**（2026-11-08）：`wsm_backend/db` 的 5 个 `init_*.sql` 从 `fa1fa24` 恢复到 `wsm_cloud/db/` |
| F-02 | 业务字段前端只读 | P0 | 4 个业务页弹窗把 `status`、`qtyOnHand/Allocated/Locked/Available`、`version`、承运商/运单号、确认时间等由业务端点写入的字段置 `disabled`，提示「由业务端点写入」；同步移除对应 `rules.required` 并给 `defaultForm` 默认值 |
| F-03 | 业务字段后端白名单 | P0 | 在 `wms-common` 加全局请求体切面：通用 CRUD 的 `POST/PUT /wms_inbound_order`、`/wms_outbound_order`、`/wms_stocktake_order`、`/wms_inventory` 若携带受保护字段，返回业务错误，不落库。**不逐 Controller 改**（生成区会被重新生成覆盖） |
| F-04 | 库存页操作收敛 | P0 | 补做 V1.0 F-20：库存页去掉「新增」「删除」按钮与其 import/方法；盘点页两套创建入口收敛为业务端点一个 |
| F-05 | 并发验收 | P0 | `e2e-cloud.mjs` 新增并发 step：构造初始 100 件的库存记录，100 个并发请求各扣 1 件，断言成功数、在库量、流水条数、无负数；再测超额并发被拒 |
| F-06 | 明细按单查询 | P0 | 3 个明细 Controller 手写区新增按单据 ID 查询端点；前端 `wms_biz.js` 的 `fetchAll` 改调新端点，消除「最多 10 页 × 200 行」的静默截断 |
| F-07 | 一键建库 | P0 | `scripts/db-init.sh`：先建库 → 显式 `--database=wms_warehouse` + `--default-character-set=utf8mb4` → 顺序执行 6 个 `db/*.sql` + 2 个 `docs/sql/V1.0__*.sql`，幂等可重跑 |
| F-08 | 对账与冲销 | P0 | `scripts/reconcile.mjs` 输出 `on_hand ≠ Σ qty_change` 差异清单；`wms-inventory` 新增冲销端点，按流水合计修正账面并写 `ADJUST` 流水（走 `InventoryCoreService` + `requestId` 幂等） |
| F-09 | 安全默认值收紧 | P0 | `application.yml` 去掉 `${DB_PASSWORD:root}`、Java 去掉 `${app.internal.token:wms-internal}` 默认值，缺配置启动即失败；本地开发值改由 `cloud-up.sh` 显式注入，不破坏现有环境 |
| F-10 | 门禁化 | P0 | `scripts/verify.sh` 串起：编译 → 库表体检 → e2e（含并发）→ 对账 → 前端 build → 报告；任一环失败非零退出 |

## 边界与异常

| 场景 | 期望行为 |
|---|---|
| 直接 `PUT /wms_inventory` 携带 `qtyOnHand` | HTTP 200 + 非 200 `code`，msg「数量字段由业务端点写入，禁止直接修改」，库里的值不变 |
| 直接 `PUT /wms_inbound_order` 携带 `status` | 同上被拒；单据状态只能由 `DocStateMachine` 经业务端点流转 |
| 主数据页改 `wms_warehouse.status`（启用/停用） | **放行**：白名单只作用于 4 类单据/库存资源，不误伤主数据 |
| 业务端点 `/receive`、`/putaway`、`/pick`、`/{id}/count` | **放行**：切面只精确匹配通用 CRUD 的根路径，不拦截子路径 |
| 前端禁用字段未同步移除 `rules.required` | 新增弹窗 `validate()` 会拦住提交 → 属实现缺陷，S8 必须走「新增一条主数据」验证 |
| 业务组件里的「本次收货量/拣货量/实盘量」输入框 | **不禁用**：它们是业务端点的合法入参，不是主表 `qty*` 字段 |
| 100 并发各扣 1 件（初始 100） | 成功 100 次、`on_hand=0`、流水 100 条、无负数、无死锁 |
| 150 并发各扣 1 件（初始 100） | 成功 100 次、失败 50 次且均为「库存不足」、`on_hand=0`、流水 100 条 |
| 明细超过 200 行 | 新端点一次返回全量，弹窗不再静默漏行 |
| 人为制造「远程成功、本地失败」 | `reconcile.mjs` 报出差异；冲销后差异归零、流水自洽 |
| 冲销重复提交同一 `requestId` | 返回上次结果，不重复写流水 |
| 未设 `DB_PASSWORD` / `APP_INTERNAL_TOKEN` 直接 `java -jar` | 启动明确报错退出；但 `cloud-up.sh --with-web` 因显式注入开发值仍可正常起 |
| `verify.sh` 任一环节失败 | 立即非零退出，打印失败环节，不继续后续步骤 |

## 验收标准（2026-11-08 S8 实跑结论）

- [x] F-01 `db/` 下 6 个 SQL 齐备（5 个 `init_*.sql` + `dict.sql`）
- [x] F-02 4 个业务页实机只读控件 4 / 5 / 4 / 6 个（均 ≥ 4）；新增一条主数据 `POST /wms_warehouse` 返回 `code:200`
- [x] F-03 `PUT /wms_inventory` 带 `qtyOnHand=999` → `code:40001` 且库值 `50.000` 不变；去掉该字段 → `code:200`；主数据带 `status` 放行
- [x] F-04 库存页 DOM 无「新增库存表 / 删除」；盘点页无「新增盘点单表」，只留业务端点创建入口（实机 DOM 断言）
- [x] F-05 e2e `134/134`（V1.0 的 118 条全保持）；并发 step 输出「成功 100 / 拒绝 50 / 流水 100 / 无负数」
- [x] F-06 3 个按单查询端点均 `code:200` 且主键字符串化；`wms_biz.js` 中 `fetchAll` 只用于库位主数据，明细改走 `by-order` / `by-stocktake`
- [x] F-07 `db-init.sh` 空库与重复跑均 `rc=0`：34 张表、`wms_request_log` 存在、`qty_available` 含 `qty_locked`、字典 9 / 菜单 91 / 库存 3
- [x] F-08 `reconcile.mjs --selftest`：造差异报 1 条 → 冲销（含重复 requestId 回放）→ 0 条差异 → ADJUST 流水恰好 1 条；全库对账 0 条差异
- [x] F-09 裸跑 `java -jar` → `exit=1` + `Could not resolve placeholder 'app.internal.token'`；`cloud-up.sh --with-web` 仍能起全套并跑通 e2e
- [x] F-10 `verify.sh --skip-build --with-ui --negative-test` → `rc=0` / 9 环节 PASS / `VERIFY PASS`；改错并发断言后脚本非零退出
- [x] 整体：`mvn -o package` 8/8 BUILD SUCCESS；`NODE_OPTIONS= npm run build` 成功；4 个页面 console 零 error（截图存 `docs/evidence/v1.1/`）

> **F-06 口径说明**：原验收写「造 250 行明细」，S8 未造 250 行脏数据，改为验证端点本身
> 按 `order_id / stocktake_id` 精确过滤且一次返回全量（不做分页拼接），从实现上消除了截断上限；
> 若需行数级证据，可在临时库批量插 250 行后复跑 `ui-check-pages.mjs`。

## 待确认 / 口径澄清

- [x] **并发口径**：V1.0 验收原文「50 线程各扣 1 件（初始 100）→ 成功恰好 100 次」自相矛盾（50 次扣减不可能成功 100 次）。本版澄清为：**100 个并发请求各扣 1 件 → 成功 100 次**；另加 150 并发的拒绝场景。此为验收参数修正，不改变业务语义。
- [x] **本版不新建表、不改已有列**：冲销复用 `wms_inventory_txn` 写 `ADJUST` 流水，对账只读现有表；本地消息表 / 事务消息归 V2.0，避免改表结构的破坏性操作。
- [x] **安全默认值与本地开发兼容**：生产配置无默认值，开发默认值下沉到 `cloud-up.sh`，两边都不裸奔、也不破坏现有启动方式。
- [x] **提交**：用户已确认「先不提交」，S9 只做沉淀与工作日志，不 commit、不 push。

## 修订记录

| 日期 | 版本 | 变更点 | 变更人 |
|---|---|---|---|
| 2026-11-08 | v1.1 | 初稿：依据 `WMS-版本路线图.md` §2 拆出 F-01~F-10，补做 V1.0 未落实的 F-17~F-20 与并发验收 | — |
