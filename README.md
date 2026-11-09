# wms_warehouse-cloud — WMS 仓储微服务版（V8）

由单体 `wms_warehouse-server`（代码生成器产物，21 张业务表）拆分而来。
技术栈：Spring Boot 3.2.5 + Spring Cloud 2023.0.3 + Spring Cloud Alibaba 2023.0.1.3
（Nacos 注册中心 + Spring Cloud Gateway + OpenFeign），数据库与单体共用 `wms_warehouse` 库。

## 模块

| 模块 | 端口 | 职责 | 业务表 |
|---|---|---|---|
| wms-common | - | 公共基座：Result/BaseEntity/TokenStore/AuthContextFilter/日志切面/数据权限 | - |
| wms-system | 8141 | 登录鉴权（sys_user/sys_session/验证码）、字典、操作/登录日志、内置流程 | sys_* |
| wms-master | 8142 | 基础资料 | wms_warehouse/zone/location/owner/sku |
| wms-inbound | 8143 | 入库作业 | wms_inbound_order(+item)/receipt_record/putaway_task |
| wms-outbound | 8144 | 出库作业 | wms_outbound_order(+item)/pick_task/wave/shipment |
| wms-inventory | 8145 | 库存中心 | wms_inventory/lock/txn/allocation/stocktake_*/transfer_order |
| wms-gateway | 8100 | 统一入口，按路径前缀路由（lb://服务名） | - |

## 鉴权设计（拆分零改造的关键）

单体鉴权是 **DB 会话式**：`/auth/login` 签发 token 写 `sys_session`，
各请求由 `AuthContextFilter` + `TokenStore`（内存缓存 60s + 回源查库）校验。
拆分后所有服务共库，`sys_session` 谁都能查 → **网关不鉴权、各业务服务本地校验**，
公共鉴权代码原样下沉 wms-common，一行没改。

## 业务闭环（V1.0，2026-11-07 从单体迁移）

> 版本路线图：[`docs/WMS-版本路线图.md`](./docs/WMS-版本路线图.md)（V1.0 基线 → V1.1 可信闭环 → V1.2 作业补全 → V2.0 生产可用）。

单据从「能录入」变成「有因果」：收货 → 上架 → 分配 → 拣货 → 发运，盘点的冻结 → 实盘 → 调整。
代码按**归属**拆到各服务，库存的写入口只存在于 wms-inventory 一个进程内。

| 能力 | 位置 | 说明 |
|---|---|---|
| 状态机 / 数量工具 / 错误码 | wms-common `common.statemachine` `common.util` `common.result` | 三张单据共用一套流转表，非法流转直接拒绝 |
| 幂等键（wms_request_log） | wms-common `common.idempotent` | 各服务共用一张表，重复提交返回上次结果 |
| 库存唯一入口 `InventoryCoreService` | wms-inventory | 六个方法：increase/decrease/allocate/releaseAllocate/lock/unlock，每个都写 `wms_inventory_txn` |
| FEFO 分配 / 拣货占用 | wms-inventory `InventoryAllocationService` | 分配策略与 `wms_allocation` 明细都归库存中心 |
| 盘点闭环 | wms-inventory `WmsStocktakeBizService` | 快照 → 冻结 → 实盘 → 先解冻再调整 |
| 入库闭环 | wms-inbound `WmsInboundBizService` | 收货累加、上架加库存，经 Feign 调库存服务 |
| 出库闭环 | wms-outbound `WmsOutboundBizService` | 分配/拣货/发运/取消分配，经 Feign 调库存服务 |
| 主数据内部查询 | wms-master `InternalMasterController` | 库位是否存在、SKU 是否批次管理/保质期 |

### 内部接口（网关不路由，仅内网可达）

| 接口 | 提供方 | 消费方 |
|---|---|---|
| `POST /internal/inventory/increase` `/decrease` `/allocate` `/release-allocate` `/lock` `/unlock` | wms-inventory | 入库、出库 |
| `POST /internal/inventory/allocate-item` `/pick-item` `/release-order` | wms-inventory | 出库 |
| `POST /internal/inventory/fefo-candidates` | wms-inventory | 只读排查用（看某 SKU 的候选批次与可用量） |
| `GET /internal/master/location/{id}` `/sku/{id}` `/zone/{id}/locations` | wms-master | 入库、库存 |

`/internal/**` 已加入 `AuthContextFilter` 白名单（服务间调用没有用户 token），
入口再校验请求头 `X-Internal-Token`，值取配置 `app.internal.token`。
**V1.1 起该配置与 `DB_PASSWORD` 都不带默认值**——公开仓库里不再存明文口令/令牌，
缺配置启动即失败；本地开发值由 `scripts/cloud-up.sh` 显式注入（`APP_INTERNAL_TOKEN` /
`DB_PASSWORD`，可用环境变量覆盖）。网关路由表里没有 `/internal`，外部打不进来；
生产环境应换成 mTLS 或签名。

### 一致性边界（必须知道的取舍）

入库/出库的本地事务只覆盖自己的表，库存写入是一次**独立的远程提交**，没有 XA：

- 远程失败 → 本地事务回滚，账面一致（常态）；
- 远程成功但本地提交失败 → 会多出一笔库存。共库拆分的固有缺口，V1.1 提供
  `scripts/reconcile.mjs` 对账 **+ `/internal/inventory/reconcile-adjust` 冲销端点**修正；
- 对账口径是**增量**的：比对 `qty_on_hand` 与「该库存最近一条流水的 `qty_after`」。
  不是 `on_hand = Σ 全量流水`——seed 库存有期初量却无期初流水，按绝对值算会报假差异；
- 生产环境应改为事务消息（RocketMQ）或本地消息表，本版保留这个缺口并显式记录。

## 启动（Windows Git Bash）

```bash
# 0. 一键建库（空库可跑、重复跑不报错；显式 utf8mb4，中文种子不会 ERROR 1366）
#    顺序：5 个 init_*.sql + dict.sql + 2 个 V1.0 增量脚本，末尾做表数量/生成列/字典体检
bash scripts/db-init.sh
#    DB_PASS=xxx DB_USER=xxx DB_NAME=xxx 可覆盖；--with-legacy-seed 才导入生成器随机数据
#    Nacos 单机模式由 cloud-up.sh 检测，没起会自动拉起
# 1. 打包。注意先停服务：正在运行的 java 进程会锁住 target/*.jar，
#    Windows 下 spring-boot repackage 会因无法 rename 成 .original 直接失败
bash scripts/cloud-down.sh --with-web
mvn package -DskipTests
# 2. 一键启动 Nacos + 6 个服务 + 前端 dev server（在你自己开的 Git Bash 里前台跑）
bash scripts/cloud-up.sh --with-web            # http://127.0.0.1:9040
# 3. 停止
bash scripts/cloud-down.sh --with-web         # 停 6 服务 + 前端，留着 Nacos
bash scripts/cloud-down.sh --with-nacos --with-web   # 全停
bash scripts/cloud-down.sh --only 8143 8144 8145    # 重启打包前只停指定端口
```

**重要：不要用对话里的后台任务托管这套环境。** 会话/对话结束时后台进程会被宿主回收，
页面会突然全部「请求失败」（代理连不上 8100，axios 无 HTTP 响应）。
把 `cloud-up.sh --with-web` 放在你自己的终端里前台保活即可；`cloud-up.sh` 会检测
已监听端口并跳过，重复执行不会起第二份。

### 页面请求失败快速排查

| 现象 | 检查 |
|---|---|
| 全部列表空 + 「无法连接后端网关（8100）」 | `netstat -ano \| grep -E ':8100\|:9040'`，没监听就跑 `bash scripts/cloud-up.sh --with-web` |
| 登录后就跳回登录页 / 401 | `sys_session` 里的 token 过期，重新登录 admin / admin123 |
| 首页「后端连通检测」失败 | 网关需有 `/health` 路由（已配），wms-system 需有 `HealthController`（已加）；直连 8141/health 与经 8100/health 各打一次对照 |
| 单个页面 404 | 前端 api 的 `/api/demo` 前缀由 `vue.config.js` 的 `pathRewrite` 剥掉，网关按 `/wms_xxx/**` 路由；对照 `wms-gateway/application.yml` |
| **带字典的页面**弹「请求失败」但接口 HTTP 200 | 看响应体是不是裸数组 `[...]`（无 `code`）。`SysDictController` 必须 `Result.ok(...)`，且 `sys_dict_data` 表要存在（灌 `db/dict.sql`）。已修，见坑库《spring-boot-3》字典只读接口条目 |
| HTTP 200 但 `code:500 服务异常` | 先看后端日志 `Caused by`；注意生成器分页是 **POST** `/page`，用 GET 探测会撞 `GET /{id}` 报 `For input string: "page"` 的假 500 |
| 网关 503 但直连服务正常 | Nacos 注册 IP/端口不对，启动参数必须带 `--spring.cloud.nacos.discovery.ip=127.0.0.1` |

`cloud-up.sh` 显式处理了两个运行时坑（都是实测踩过的）：

- 每个进程单独设 `SERVER_PORT` 并加 `--server.port`：宿主注入的 `SERVER_PORT` 会被 Spring Boot
  宽松绑定成 `server.port`，优先级高于配置，Nacos/各服务会去抢桌面端已占用的端口而启动失败；
- `--spring.cloud.nacos.discovery.ip=127.0.0.1`：多网卡机器注册到虚拟网卡 IP 时，网关按注册
  地址转发必然 503。启动后可查 `nacos/v1/ns/instance/list` 核对注册 ip/port。

登录：admin / admin123（与单体同一套种子数据）。

## 端到端验证

```bash
node scripts/e2e-cloud.mjs          # 默认跑完把 seed 现场恢复，可反复运行
node scripts/e2e-cloud.mjs --keep   # 保留结果，便于人工查库
```

需要覆盖时可用环境变量：`GATEWAY_URL` / `INVENTORY_URL` / `MYSQL_BIN` / `DB_USER` / `DB_PASS` / `DB_NAME`。

脚本只操作 V1.0 seed 的 1001 号入库单/出库单与 3 行库存，开跑前把这几行重置回初始值，
其余演示数据不碰；登录接口的验证码是图形算术题，自动化不可解，因此按集成测试同构方式
向 `sys_session` 插一条 admin 会话，`AuthContextFilter` / `PermissionChecker` 全链路仍是真实的。

覆盖内容（2026-11-07 连跑两次，**118/118 断言通过**）：

1. 网关路由 + 401 鉴权 + `/internal/**` 不路由 + 内部令牌 403；
2. 入库：收货 → 重复提交回放 → 越级上架拦截 → 上架新增库存/流水；
3. 出库：FEFO 分配（按数据库效期正序核对切片）→ 取消分配释放占用 → 重新分配 → 拣货扣在库 → 发运 → 重复发运回放；
4. 盘点：范围快照 → 冻结（可用量归零）→ 录实盘（生成列 diff）→ 确认调整（先解冻再盘亏）；
5. 全局一致性：每条流水 `qty_after - qty_before = qty_change`、按业务键「账面变化 = 流水合计」、
   `qty_available` 生成列恒等式、三张单据终态。

## V1.1 可信闭环（2026-11-08）

本版不加业务功能，把 V1.0 的「能跑通」升级为「不可绕过、可证明、可重建」。完整需求/设计/验证证据见
`docs/requirements/WMS-V1.1-可信闭环-需求.md`、`docs/design/WMS-V1.1-可信闭环-设计.md`。

**前后端双层只读**：4 个业务页的 `status` / `qty*` / `version` / 承运信息等控件置 `disabled`，
同步删掉对应的 `rules.required`，提交前用 `src/utils/readonly.js` 剔除受保护字段；
后端在 `wms-common` 用全局 `RequestBodyAdvice` 对 4 类资源根路径的 POST/PUT
「携带受保护字段即拒绝」（`code:40001`）。主数据与业务子路径放行。

**脚本一览**

| 脚本 | 用途 |
|---|---|
| `scripts/db-init.sh` | 一键建库（空库/重复跑均通过），含 `sys_role_menu.deleted` 兼容列与菜单去重 |
| `scripts/e2e-cloud.mjs` | V1.0 全部断言 + 100/150 并发防超卖，共 **134/134**；`E2E_REPORT_FILE` 可导出 JSON 报告 |
| `scripts/reconcile.mjs` | 增量对账；`--selftest` 自包含验证造差异→冲销→归零→清理，`--fix` 可批量修正 |
| `scripts/ui-check-pages.mjs` | 实机打开 4 页，抓 console/网络错误并断言只读控件与按钮收敛 |
| `scripts/verify.sh` | 一条命令串起编译→库体检→e2e→对账→前端 build→UI→报告；`--negative-test` 负向验证 |

```bash
bash scripts/verify.sh --skip-build --with-ui --negative-test   # 服务在跑时的完整门禁
bash scripts/verify.sh                                          # 停服后可顺带验证编译
```

**凭据**：`application.yml` 的 `${DB_PASSWORD}` 与 Java 的 `${app.internal.token}` 无默认值，
裸跑 `java -jar` 会明确报 `Could not resolve placeholder`；只有 `cloud-up.sh` 注入本地开发值。
生产部署请用环境变量覆盖。

## 前端 V1.0 业务闭环（S7）

前端在兄弟工程 `wms_warehouse-web`，代理已指向本网关 8100。4 个生成页只在页头与表格操作列
各插入一行组件标签，业务逻辑集中在 `src/components/biz/` 的 5 个组件里，接口统一走
`src/api/wms_biz.js`（10 个业务端点 + `requestId` 幂等键生成 + 明细全量筛选）：

| 页面 | 接入动作 |
|---|---|
| 入库单 | 流程条、收货登记（逐行本次数量）、上架（逐行库位 + 数量） |
| 出库单 | 流程条、分配（结果弹窗展开 FEFO 批次切片）、取消分配、拣货、发运 |
| 盘点单 | 流程条、新建盘点单（按仓库/库位圈范围）、冻结、录实盘、确认调整 |
| 库存 | 恒等式提示面板、查看流水、效期状态列、刷新 |

`npm run build` 通过；无头浏览器实测 4 个页面均正常渲染（0 运行时错误），点击「收货」能打开
收货登记弹窗。~~V1.0 已知边界：明细按 10 页 × 200 行拉取会静默截断~~ —— **V1.1 已修**：
新增 `by-order/{orderId}` / `by-stocktake/{stocktakeId}` 三个端点，`wms_biz.js` 一次取全量，
不再有行数上限。

## 更新记录

- 2026-11-08 V1.1 可信闭环（全栈流水线 S0~S9，用户确认「先不提交」）：
  - 前后端双层字段只读 + 库存/盘点页操作收敛 + 明细按单查询端点
  - 并发验收（100/150 并发，e2e 134/134）、增量对账与幂等冲销、一键建库、安全默认值、门禁化
  - 实机证据：`docs/evidence/v1.1/` 4 张截图；`verify.sh --with-ui --negative-test` 9 环节全绿
- 2026-11-07 V1.0 端到端验证 + 前端接入：
  - 新增 `scripts/cloud-up.sh` / `cloud-down.sh` / `e2e-cloud.mjs`，e2e 连跑两次 118/118 通过，
    覆盖三条闭环、FEFO、幂等回放、流水恒等式与网关/内部接口边界；
  - 修复 `Map<String,Object>` 返回值里的雪花 ID：`LongIdJacksonConfig` 的
    `BeanSerializerModifier` 只作用于 POJO 属性，Map value 里的 `Long` 仍以 JSON number 下发，
    前端 JS 会静默丢精度（盘点单创建后拿 id 查不到明细）。业务结果里的 ID 一律显式
    `String.valueOf(...)`，e2e 里加了 `typeof === 'string'` 防回归断言；
  - 前端 4 个页面接入 10 个业务端点（代码在 `wms_warehouse-web`，本仓库只记录对照）。
- 2026-11-07 V1.0 业务闭环迁移：单体 `wms_warehouse-server` 里新写的收货/上架/分配/拣货/发运/盘点代码
  按归属拆入 cloud——
  - `wms-common`：BizCode、DocStateMachine、QtyUtils、WmsRequestLog + 幂等服务、内部调用 DTO（`common.api`）
  - `wms-inventory`：InventoryCoreService（库存唯一写入口）、InventoryAllocationService（FEFO 分配/拣货）、
    盘点闭环、`InternalInventoryController`
  - `wms-inbound`：入库闭环 + `InventoryClient`/`MasterClient`
  - `wms-outbound`：出库闭环 + `InventoryClient`（分配/拣货/释放整单占用）
  - `wms-master`：`InternalMasterController`（库位/SKU/库区库位查询）
  - 各服务 `@MapperScan` 增加 `com.wms_warehouse.common.idempotent`
- 2026-11-05 V8.0 拆分完成：单体零跨域引用（controller/service 均单域依赖），
  按域整体搬迁 + OpeLog/OpeLogMapper 下沉 wms-common + MapperScan 加扫 common.log 包。


---

## 关于这份源码快照

- **只含源码与文档**：已排除 `target/`、`logs/`、`*.jar`、本地备份与诊断脚本等构建产物和本机私有文件，clone 后需自行 `mvn clean package -DskipTests` 构建。
- 保留 `wms-common/src/main/resources/ip/ip2region.db`，它是运行时必需的 IP 库。
- **配置中的口令是本地开发默认值**（如 `root`、`wms-internal`），不是任何真实环境的凭据；生产部署务必通过环境变量覆盖。
- 本快照是工作区当前文件状态的一次提交，包含本地开发中的最新改动，但不含中间提交历史。
