<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* V6.8 品牌主色：唯一来源是 sys_project.app_theme_color（空则 #e1251b），脚手架只替换这一个变量。
   App 与 layout 各声明一份（懒加载 chunk 的 CSS 注入时序不保证，登录页要先于 Layout 出现） */
:root {
  --app-primary: #e1251b;
  --app-primary-soft: rgba(225, 37, 27, 0.1);
}
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ========== Element UI 全局打磨 ========== */

/* 卡片：圆角 + 轻阴影 */
.el-card {
  border-radius: 12px;
  border: 1px solid #eef1f7;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.04) !important;
}
.el-card__header {
  border-bottom: 1px solid #f0f3f9;
  font-weight: 600;
  color: #1f2937;
}

/* 表格 */
.el-table {
  border-radius: 10px;
  overflow: hidden;
}
.el-table th {
  background: #f7f9fc !important;
  color: #47536b !important;
  font-weight: 600;
}
.el-table td,
.el-table th.is-leaf {
  border-bottom: 1px solid #f0f3f9;
}
.el-table--enable-row-hover .el-table__body tr:hover > td {
  background: #fafcff;
}
.el-table .caret-wrapper {
  display: none;
}
.el-table .sort-caret.ascending {
  border-bottom-color: #c0c4cc;
}

/* 按钮：主按钮渐变、统一圆角 */
.el-button {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.16s ease;
}
.el-button--primary {
  background: linear-gradient(135deg, var(--app-primary) 0%, #ff5a3c 100%);
  border: none;
  box-shadow: 0 3px 10px rgba(225, 37, 27, 0.25);
}
.el-button--primary:hover,
.el-button--primary:focus {
  background: linear-gradient(135deg, #ff3b2e 0%, #ff7a54 100%);
  transform: translateY(-1px);
  box-shadow: 0 5px 14px rgba(225, 37, 27, 0.32);
}
.el-button--primary.is-plain,
.el-button--primary--text {
  background: #fff;
}
.el-button--mini,
.el-button--small {
  border-radius: 7px;
}

/* 输入框 / 选择器 */
.el-input__inner,
.el-textarea__inner {
  border-radius: 8px;
  border-color: #e3e8f2;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}
.el-input__inner:hover,
.el-textarea__inner:hover {
  border-color: #c9d3e8;
}
.el-input__inner:focus,
.el-textarea__inner:focus {
  border-color: var(--app-primary);
  box-shadow: 0 0 0 3px rgba(225, 37, 27, 0.08);
}

/* 分页 */
.el-pagination {
  padding: 14px 0 2px;
}
.el-pagination.is-background .el-pager li:not(.disabled).active {
  background: var(--app-primary);
  border-radius: 6px;
}
.el-pagination.is-background .el-pager li {
  border-radius: 6px;
}

/* 对话框 */
.el-dialog {
  border-radius: 14px;
  overflow: hidden;
}
.el-dialog__header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f3f9;
}
.el-dialog__title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}
.el-dialog__body {
  padding: 20px;
}

/* 标签 */
.el-tag {
  border-radius: 6px;
}

/* 消息提示圆角 */
.el-message {
  border-radius: 10px;
  border: none;
  box-shadow: 0 6px 20px rgba(16, 24, 40, 0.12);
}

/* 下拉菜单 */
.el-dropdown-menu {
  border-radius: 10px;
  border: 1px solid #eef1f7;
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.12);
  padding: 6px;
}
.el-dropdown-menu__item {
  border-radius: 7px;
  line-height: 30px;
}
.el-dropdown-menu__item:not(.is-disabled):hover {
  background: #f5f7fd;
  color: var(--app-primary);
}

/* tooltip 跟随菜单栏深色风格 */
.el-tooltip__popper.is-dark {
  background: #232d47;
}
</style>
