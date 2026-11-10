<template>
  <div class="layout" :class="{ 'is-collapsed': collapsed }">
    <!-- 左侧固定侧边栏 -->
    <aside class="sider">
      <!-- 品牌 -->
      <div class="brand" @click="go('/home')">
        <div class="brand-mark">w</div>
        <transition name="fade-x">
          <div v-if="!collapsed" class="brand-text">
            <div class="brand-name">wms_warehouse</div>
            <div class="brand-sub">仓储管理系统</div>
          </div>
        </transition>
      </div>

      <!-- 菜单：Element UI 手风琴，展开/折叠/折叠态悬停右滑弹出全部由组件保证 -->
      <div class="menu-scroll">
        <el-menu
          ref="menu"
          :collapse="collapsed"
          :collapse-transition="false"
          :default-active="$route.path"
          router
          class="sider-menu"
        >
          <el-menu-item index="/home" class="menu-home">
            <i class="el-icon-s-home"></i>
            <span slot="title">首页</span>
          </el-menu-item>
          <el-submenu v-for="grp in sections" :key="grp.path" :index="grp.path">
            <template slot="title">
              <i :class="grp.icon" class="group-icon"></i>
              <span class="group-title">{{ grp.title }}</span>
              <span class="group-count" @click.stop>{{ grp.pages.length }}</span>
            </template>
            <el-menu-item v-for="p in grp.pages" :key="p.path" :index="p.path" class="menu-page">
              <span class="page-dot"></span>
              <span slot="title" class="page-name">{{ p.title }}</span>
            </el-menu-item>
          </el-submenu>
        </el-menu>
      </div>

      <!-- 底部：折叠开关 -->
      <div class="sider-foot">
        <div class="menu-page foot-item collapse-btn" @click="collapsed = !collapsed">
          <i class="foot-icon" :class="collapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          <span v-if="!collapsed" class="page-name">收起菜单</span>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <div class="header">
        <div class="header-left">
          <el-breadcrumb separator="/" class="crumbs">
            <el-breadcrumb-item v-for="(crumb, i) in crumbs" :key="i">{{ crumb }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="刷新当前页" placement="bottom">
            <button class="hbtn" @click="refreshPage"><i class="el-icon-refresh-right"></i></button>
          </el-tooltip>
          <span class="env-tag">DEV</span>
          <el-dropdown trigger="click" @command="onUserCmd">
            <div class="user-chip">
              <span class="avatar">{{ (userName || '管').charAt(0) }}</span>
              <span class="user">{{ userName }}</span>
              <i class="el-icon-arrow-down user-arrow"></i>
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="logout" icon="el-icon-switch-button">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
/**
 * 左侧固定侧边栏：分组（业务域）→ 页面，用 el-menu 手风琴实现。
 * 菜单树直接从路由表推导（this.$router.options.routes），
 * 路由与菜单永远同源，不需要再维护一份菜单配置；
 * 也不能静态 import 路由表——gen-routes 与本组件互相引用会形成循环依赖，
 * 一旦加载顺序不利，Layout 会被解析成 undefined，所有生成页面直接白屏。
 */
function buildMenu(routes, parentPath) {
  const list = []
  ;(routes || []).forEach(route => {
    if (!route || route.hidden) return
    const path = route.path && route.path.charAt(0) === '/'
      ? route.path
      : (parentPath + '/' + route.path).replace(/\/{2,}/g, '/')
    const children = buildMenu(route.children, path)
    // 只保留「有子节点」或「有实际页面」的节点，Layout 壳子本身不进菜单
    const hasComponent = typeof route.component === 'function'
    if (!children.length && !hasComponent) return
    list.push({
      path,
      title: (route.meta && route.meta.title) || route.name || path,
      children
    })
  })
  return list
}

const ICONS = [
  'el-icon-s-platform',
  'el-icon-s-cooperation',
  'el-icon-s-shop',
  'el-icon-s-data',
  'el-icon-s-check',
  'el-icon-s-tools'
]

export default {
  name: 'Layout',
  data() {
    return {
      userName: localStorage.getItem('username') || '管理员',
      collapsed: localStorage.getItem('menu_collapsed') === '1'
    }
  },
  computed: {
    menus() {
      return buildMenu(this.$router.options.routes, '')
        // 顶层 '/' 是 Layout 壳、'/home' 侧栏已有独立入口、'/login' 不该进菜单（它混进来会导致
        // menus.length>1，业务域被当成叶子页面项、点击跳到空 GroupView——菜单"点不开"的根因）
        .filter(m => m.path !== '/' && m.path !== '/home' && m.path !== '/login')
    },
    /*
     * 侧栏分组：只有 1 个系统时，直接用它的业务域做一级分组（避免多套一层无意义的壳）；
     * 多系统时每个系统自身就是一个分组。
     */
    sections() {
      const withIcons = list => list.map((m, i) => ({
        path: m.path,
        title: m.title,
        icon: ICONS[i % ICONS.length],
        pages: m.children && m.children.length
          ? m.children.map(p => ({ path: p.path, title: p.title }))
          : [{ path: m.path, title: m.title }]
      }))
      if (this.menus.length === 1 && this.menus[0].children.length) {
        return withIcons(this.menus[0].children)
      }
      return withIcons(this.menus)
    },
    crumbs() {
      const list = this.$route.matched
        .map(item => (item.meta && item.meta.title) || '')
        .filter(Boolean)
      return list.length ? list : ['首页']
    }
  },
  watch: {
    '$route.path'() {
      // 当前路由所在分组自动展开
      this.$nextTick(() => {
        const hit = this.sections.find(s => s.pages.some(
          p => this.$route.path === p.path || this.$route.path.indexOf(p.path + '/') === 0
        ))
        if (hit && this.$refs.menu && this.$refs.menu.openedMenus.indexOf(hit.path) === -1) {
          this.$refs.menu.open(hit.path)
        }
      })
    },
    collapsed(v) {
      localStorage.setItem('menu_collapsed', v ? '1' : '0')
    }
  },
  mounted() {
    // 进入页面时展开当前路由所在分组；URL 带 ?_menu=1 时展开全部，便于演示与自动化验证
    this.$nextTick(() => {
      if (this.$route.query._menu) {
        this.sections.forEach(s => this.$refs.menu.open(s.path))
        return
      }
      const hit = this.sections.find(s => s.pages.some(
        p => this.$route.path === p.path || this.$route.path.indexOf(p.path + '/') === 0
      ))
      if (hit) this.$refs.menu.open(hit.path)
    })
  },
  methods: {
    go(path) {
      if (path && this.$route.path !== path) this.$router.push(path)
    },
    refreshPage() {
      window.location.reload()
    },
    onUserCmd(cmd) {
      if (cmd === 'logout') {
        localStorage.removeItem('token')
        localStorage.removeItem('perms')
        this.$router.push('/login')
      }
    }
  }
}
</script>

<style>
/* V6.8 品牌主色：唯一来源是 sys_project.app_theme_color（空则 #e1251b），脚手架只替换这一个变量 */
:root {
  --app-primary: #e1251b;
  --app-primary-soft: rgba(225, 37, 27, 0.09);
  --sider-width: 232px;
  --sider-collapsed: 64px;
}
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f5f7fb;
}

/* ---------- 侧边栏 ---------- */
.sider {
  flex: 0 0 var(--sider-width);
  width: var(--sider-width);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #edf0f6;
  transition: flex-basis 0.22s cubic-bezier(0.22, 0.61, 0.36, 1), width 0.22s cubic-bezier(0.22, 0.61, 0.36, 1);
  z-index: 20;
}
.layout.is-collapsed .sider {
  flex-basis: var(--sider-collapsed);
  width: var(--sider-collapsed);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  padding: 0 14px;
  border-bottom: 1px solid #f0f3f9;
  cursor: pointer;
  overflow: hidden;
  white-space: nowrap;
  flex: 0 0 auto;
}
.brand-mark {
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  line-height: 34px;
  text-align: center;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--app-primary) 0%, #ff6a3d 100%);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  box-shadow: 0 4px 12px rgba(225, 37, 27, 0.32);
}
.brand-text {
  min-width: 0;
}
.brand-name {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: 0.2px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.brand-sub {
  margin-top: 1px;
  font-size: 10px;
  color: #a5aec4;
  letter-spacing: 2px;
}

.menu-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 10px;
}
.menu-scroll::-webkit-scrollbar {
  width: 5px;
}
.menu-scroll::-webkit-scrollbar-thumb {
  background: #e2e7f0;
  border-radius: 3px;
}

/* ---------- el-menu 定制 ---------- */
.sider-menu {
  border-right: none !important;
  background: transparent !important;
}
.sider-menu .el-submenu__title,
.sider-menu .el-menu-item {
  height: 42px !important;
  line-height: 42px !important;
  border-radius: 10px !important;
  margin-bottom: 2px;
  color: #4b5568 !important;
  font-size: 13px !important;
  position: relative !important;
  transition: background 0.15s ease, color 0.15s ease;
}
.sider-menu .el-submenu__title {
  font-weight: 500;
}
.sider-menu .el-submenu__title:hover,
.sider-menu .el-menu-item:hover {
  background: #f5f7fd !important;
  color: #1f2937 !important;
}
.sider-menu .el-submenu.is-active > .el-submenu__title,
.sider-menu .el-submenu.is-opened > .el-submenu__title {
  color: var(--app-primary) !important;
  background: var(--app-primary-soft) !important;
}
.sider-menu .el-menu-item.is-active {
  color: var(--app-primary) !important;
  background: var(--app-primary-soft) !important;
  font-weight: 600;
}
/* 子级页面项内缩（覆盖 Element 按层级内联计算的 padding） */
.sider-menu .el-menu .el-menu-item {
  padding-left: 46px !important;
  min-width: 0 !important;
}
.group-icon {
  font-size: 16px !important;
  color: #8a94ab;
  margin-right: 8px;
}
.sider-menu .el-submenu.is-active .group-icon {
  color: var(--app-primary);
}
.group-title {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.group-count {
  position: absolute;
  right: 30px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 10px;
  height: 17px;
  line-height: 17px;
  padding: 0 6px;
  border-radius: 9px;
  background: #f0f3f9;
  color: #98a2b8;
}
.sider-menu .el-submenu.is-active .group-count {
  background: rgba(225, 37, 27, 0.14);
  color: var(--app-primary);
}
.page-dot {
  position: absolute;
  left: 30px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #cbd3e3;
}
.sider-menu .el-menu-item.is-active .page-dot {
  background: var(--app-primary);
  box-shadow: 0 0 0 3px rgba(225, 37, 27, 0.15);
}
.page-name {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
/* 折叠态：图标居中 */
.layout.is-collapsed .sider-menu .el-submenu__title,
.layout.is-collapsed .sider-menu .el-menu-item {
  padding: 0 !important;
  text-align: center;
}
.layout.is-collapsed .sider-menu .group-icon {
  margin-right: 0;
  font-size: 18px !important;
}
/* 折叠态悬停弹出的浮层菜单 */
.el-menu--vertical .el-menu--popup {
  min-width: 170px;
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.14);
}
.el-menu--vertical .el-menu--popup .el-menu-item {
  height: 38px;
  line-height: 38px;
  border-radius: 8px;
  color: #4b5568;
  font-size: 13px;
  min-width: 0;
}
.el-menu--vertical .el-menu--popup .el-menu-item:hover {
  background: #f5f7fd;
  color: #1f2937;
}
.el-menu--vertical .el-menu--popup .el-menu-item.is-active {
  color: var(--app-primary);
  background: var(--app-primary-soft);
  font-weight: 600;
}

/* 品牌文字淡入淡出 */
.fade-x-enter-active,
.fade-x-leave-active {
  transition: opacity 0.15s ease;
}
.fade-x-enter,
.fade-x-leave-to {
  opacity: 0;
}

/* 底部固定项 */
.sider-foot {
  flex: 0 0 auto;
  padding: 8px 10px 12px;
  border-top: 1px solid #f0f3f9;
}
.foot-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 10px;
  margin-bottom: 2px;
  border-radius: 9px;
  font-size: 12px;
  color: #98a2b8;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}
.foot-item:hover {
  background: #f5f7fd;
  color: #4b5568;
}
.foot-icon {
  font-size: 16px;
  color: #8a94ab;
  width: 16px;
  text-align: center;
}
.foot-item:hover .foot-icon {
  color: var(--app-primary);
}
.layout.is-collapsed .foot-item {
  justify-content: center;
  padding: 0;
}

/* ---------- 主区域 ---------- */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.header {
  flex: 0 0 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e9edf5;
  z-index: 10;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.crumbs {
  font-size: 13px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #5a6478;
}
.hbtn {
  width: 30px;
  height: 30px;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  background: #fff;
  color: #6b7590;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.16s ease;
}
.hbtn:hover {
  color: var(--app-primary);
  border-color: var(--app-primary);
  background: var(--app-primary-soft);
}
.env-tag {
  padding: 2px 9px;
  border-radius: 10px;
  background: #eef2ff;
  color: #4d7cfe;
  font-size: 11px;
  font-weight: 600;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.16s ease;
}
.user-chip:hover {
  background: #f3f5fa;
}
.avatar {
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--app-primary) 0%, #ff6a3d 100%);
}
.user {
  font-size: 13px;
  color: #3d4759;
}
.user-arrow {
  font-size: 12px;
  color: #9aa4bc;
}
.content {
  flex: 1;
  padding: 18px 20px 26px;
  overflow-y: auto;
  background: #f5f7fb;
}
.content::-webkit-scrollbar {
  width: 8px;
}
.content::-webkit-scrollbar-thumb {
  background: #d4dae6;
  border-radius: 4px;
}
.content::-webkit-scrollbar-thumb:hover {
  background: #bcc4d4;
}
</style>
