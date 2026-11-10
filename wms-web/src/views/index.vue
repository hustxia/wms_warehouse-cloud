<template>
  <div class="home">
    <!-- 欢迎横幅 -->
    <div class="banner">
      <div class="banner-text">
        <div class="banner-hi">{{ greeting }}，{{ userName }}</div>
        <div class="banner-sub">欢迎回到 {{ systemName }}，今天也保持高效运转</div>
      </div>
      <div class="banner-deco">
        <span></span><span></span><span></span>
      </div>
      <el-button class="banner-btn" round size="small" @click="ping">
        <i class="el-icon-connection"></i>
        测试后端连通
      </el-button>
    </div>
    <div v-if="result" class="ping-result" :class="{ 'is-err': pingErr }">
      <i :class="pingErr ? 'el-icon-warning-outline' : 'el-icon-success'"></i>
      {{ result }}
    </div>

    <!-- 概览卡片 -->
    <div class="stat-row">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg }">
          <i :class="s.icon"></i>
        </div>
        <div class="stat-meta">
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 业务模块快捷入口 -->
    <div class="panel">
      <div class="panel-head">
        <span class="panel-title"><i class="el-icon-menu"></i> 业务模块</span>
        <span class="panel-tip">点击直达对应功能页</span>
      </div>
      <div class="mod-grid">
        <div v-for="grp in groups" :key="grp.path" class="mod-card" @click="openGroup(grp)">
          <div class="mod-icon"><i class="el-icon-folder-opened"></i></div>
          <div class="mod-info">
            <div class="mod-name">{{ grp.title }}</div>
            <div class="mod-count">{{ grp.pages.length }} 个功能页</div>
          </div>
          <i class="el-icon-right mod-arrow"></i>
          <!-- 悬停展示组内页面 -->
          <div class="mod-pop">
            <div
              v-for="p in grp.pages"
              :key="p.path"
              class="mod-pop-item"
              @click.stop="go(p.path)"
            >
              {{ p.title }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
/**
 * 菜单树推导逻辑与 layout/index.vue 同源（从路由表读，不能静态 import gen-routes，
 * 否则与 Layout 循环依赖导致白屏——见 layout/index.vue 顶部注释）。
 */
function buildMenu(routes, parentPath) {
  const list = []
  ;(routes || []).forEach(route => {
    if (!route || route.hidden) return
    const path = route.path && route.path.charAt(0) === '/'
      ? route.path
      : (parentPath + '/' + route.path).replace(/\/{2,}/g, '/')
    const children = buildMenu(route.children, path)
    const hasComponent = typeof route.component === 'function'
    if (!children.length && !hasComponent) return
    list.push({ path, title: (route.meta && route.meta.title) || route.name || path, children })
  })
  return list
}

import request from '@/utils/request'

export default {
  name: 'Home',
  data() {
    return {
      userName: localStorage.getItem('username') || '管理员',
      result: '',
      pingErr: false
    }
  },
  computed: {
    systemName() {
      const sys = buildMenu(this.$router.options.routes, '')
        .find(m => m.path !== '/' && m.path !== '/home' && m.path !== '/login')
      return sys ? sys.title : '仓储管理系统'
    },
    groups() {
      const sys = buildMenu(this.$router.options.routes, '')
        .find(m => m.path !== '/' && m.path !== '/home' && m.path !== '/login')
      return (sys ? sys.children : []).map(g => ({
        path: g.path,
        title: g.title,
        pages: g.children && g.children.length ? g.children : [{ path: g.path, title: g.title }]
      }))
    },
    stats() {
      const pages = this.groups.reduce((n, g) => n + g.pages.length, 0)
      return [
        { label: '业务模块', value: this.groups.length, icon: 'el-icon-files', bg: 'linear-gradient(135deg,#4d7cfe,#6f8dff)' },
        { label: '功能页面', value: pages, icon: 'el-icon-document', bg: 'linear-gradient(135deg,#00b578,#3ddc97)' },
        { label: '系统数量', value: this.systemName ? 1 : 0, icon: 'el-icon-monitor', bg: 'linear-gradient(135deg,#ff9f1c,#ffbf69)' },
        { label: '运行状态', value: '正常', icon: 'el-icon-circle-check', bg: 'linear-gradient(135deg,#e1251b,#ff6a3d)' }
      ]
    },
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '夜深了'
      if (h < 9) return '早上好'
      if (h < 12) return '上午好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    }
  },
  methods: {
    ping() {
      this.result = ''
      request({ url: '/health', method: 'get' })
        .then(res => {
          this.pingErr = false
          this.result = '后端连通正常 ' + JSON.stringify(res.data)
        })
        .catch(() => {
          this.pingErr = true
          this.result = '请求失败，请检查后端服务是否启动'
        })
    },
    go(path) {
      if (path && this.$route.path !== path) this.$router.push(path)
    },
    openGroup(grp) {
      // 点击卡片进第一个页面；想选具体页可悬停看弹出列表
      const first = grp.pages[0]
      if (first) this.go(first.path)
    }
  }
}
</script>

<style scoped>
.home {
  max-width: 1280px;
  margin: 0 auto;
}

/* 欢迎横幅 */
.banner {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 26px 28px;
  border-radius: 14px;
  background: linear-gradient(120deg, var(--app-primary) 0%, #ff5a3c 55%, #ff8a3d 100%);
  color: #fff;
  box-shadow: 0 8px 24px rgba(225, 37, 27, 0.22);
}
.banner-hi {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.banner-sub {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.85;
}
.banner-btn {
  margin-left: auto;
  border: none;
  color: var(--app-primary);
  font-weight: 600;
}
.banner-deco {
  position: absolute;
  right: 150px;
  top: 0;
  bottom: 0;
  width: 200px;
  pointer-events: none;
}
.banner-deco span {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.banner-deco span:nth-child(1) { width: 130px; height: 130px; right: 10px; top: -50px; }
.banner-deco span:nth-child(2) { width: 70px; height: 70px; right: 120px; bottom: -24px; }
.banner-deco span:nth-child(3) { width: 36px; height: 36px; right: 40px; bottom: 12px; }

.ping-result {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #ecfaf1;
  color: #0f9d58;
  font-size: 12px;
  word-break: break-all;
}
.ping-result.is-err {
  background: #fdf0ef;
  color: #d93025;
}

/* 概览卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin: 16px 0;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #eef1f7;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(16, 24, 40, 0.08);
}
.stat-icon {
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  box-shadow: 0 4px 10px rgba(16, 24, 40, 0.12);
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.1;
}
.stat-label {
  margin-top: 3px;
  font-size: 12px;
  color: #8a94ab;
}

/* 业务模块 */
.panel {
  border-radius: 14px;
  background: #fff;
  border: 1px solid #eef1f7;
  padding: 16px 18px 18px;
}
.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}
.panel-title i {
  color: var(--app-primary);
  margin-right: 4px;
}
.panel-tip {
  font-size: 12px;
  color: #a5aec4;
}
.mod-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.mod-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #eef1f7;
  background: #fbfcfe;
  cursor: pointer;
  transition: all 0.18s ease;
}
.mod-card:hover {
  border-color: #ffd3ce;
  background: #fff;
  box-shadow: 0 8px 20px rgba(225, 37, 27, 0.09);
  transform: translateY(-2px);
}
.mod-icon {
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 17px;
}
.mod-name {
  font-size: 13px;
  font-weight: 600;
  color: #2b3648;
}
.mod-count {
  margin-top: 2px;
  font-size: 11px;
  color: #98a2b8;
}
.mod-arrow {
  margin-left: auto;
  color: #c6cdde;
  transition: all 0.18s ease;
}
.mod-card:hover .mod-arrow {
  color: var(--app-primary);
  transform: translateX(3px);
}

/* 悬停弹出组内页面列表 */
.mod-pop {
  display: none;
  position: absolute;
  left: 0;
  top: calc(100% + 6px);
  z-index: 20;
  min-width: 160px;
  padding: 6px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eef1f7;
  box-shadow: 0 10px 28px rgba(16, 24, 40, 0.14);
}
.mod-card:hover .mod-pop {
  display: block;
  animation: pop-in 0.16s ease;
}
@keyframes pop-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.mod-pop-item {
  padding: 8px 10px;
  border-radius: 7px;
  font-size: 12px;
  color: #4b5563;
  white-space: nowrap;
  transition: all 0.14s ease;
}
.mod-pop-item:hover {
  background: var(--app-primary-soft);
  color: var(--app-primary);
}

@media (max-width: 1100px) {
  .stat-row { grid-template-columns: repeat(2, 1fr); }
  .mod-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
