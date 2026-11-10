# WMS 仓储管理系统 · 前端

> Vue 2.7 + Element UI 的仓储管理后台，21 张业务表对应的增删改查页面。
> 由 [codegen-server](https://gitee.com/hustxiayuhong/CodeGen) 代码生成器从库表配置一键生成——表结构即页面，字段配置即表单。

## 功能模块

按「系统 → 业务域 → 页面」三级组织，菜单由后端下发，登录后动态注册路由。

| 业务域 | 页面 |
|---|---|
| 基础资料 | 仓库主表、库位表 |
| 产品管理 | 商品 SKU 表 |
| 入库管理 | 入库单主表、入库单明细、收货记录、上架任务 |
| 出库管理 | 出库单主表、出库单明细、拣货任务、发货单 |
| 库存管理 | 库存表、库存事务流水、库存锁定、库存分配明细、盘点单、盘点明细、移库单 |
| 其他 | 库区表、货主表、波次表 |

每个页面都是完整闭环：条件查询 + 分页 + 新增/编辑弹窗 + 单删/批量删除，主子表页面带明细行编辑。

## 技术栈

| 层 | 选型 |
|---|---|
| 框架 | Vue 2.7 |
| UI | Element UI 2.15 |
| 路由 | vue-router 3，`history` 模式，业务路由按角色动态注册 |
| 状态 | Vuex 3 |
| 请求 | axios 封装在 `utils/request.js` |
| 构建 | vue-cli 5（webpack） |

## 目录结构

```
src/
├── api/          21 个接口模块，与业务表一一对应（wms_*.js）
├── views/        21 个列表页（wms_*/index.vue）+ 登录页 + 首页
├── router/       index.js 常量路由 / gen-routes.js 生成的路由（勿手改）
├── layout/       侧边栏 + 主区域骨架
├── store/        Vuex
└── utils/        request.js（token 注入、code 判成败、401 回登录）
```

`src/router/gen-routes.js` 由生成器维护，注册/删除菜单后自动重建；要下线某个页面，应在表配置里删除对应表，而不是手改这个文件。

## 运行

```bash
npm install
npm run serve      # 开发，默认 http://127.0.0.1:9040
npm run build      # 生产构建到 dist/
```

开发环境 `/dev-api` 代理到 `http://127.0.0.1:8140`（见 `.env.development` 与 `vue.config.js`）。**需要先启动后端 `wms_warehouse-server`**，否则登录接口不通。

默认账号 `admin / admin123`（登录页有提示），登录需填算术验证码。

## 几个已内置的坑规避

- `vue.config.js` 开 `historyApiFallback: true` —— 否则刷新非根路径 404
- `utils/request.js` 按响应体 `code` 判成败（`0` 或 `200` 为成功），不只看 HTTP 200；401 清 token 回登录页；`blob` 响应直通、不做包装
- `toPageData()` 把 MyBatis-Plus 的 `{records,total}` 统一转成 `{list,total}` —— 页面只认一种分页结构，换后端实现不用改 21 个页面

## 说明

本仓库只含前端。后端（Spring Boot 3.2.5 + MyBatis-Plus，含登录/权限/字典基座）为同批生成产物，需配套启动。
