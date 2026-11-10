import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/index.vue'
import genRoutes from './gen-routes'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login.vue'),
    // hidden: 不进侧栏菜单树（否则登录页会被当成一个"系统"混进菜单，业务域全部降级成叶子项）
    hidden: true,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/index.vue'),
        meta: { title: '首页' }
      }
    ]
  },
  ...genRoutes
]

const router = new VueRouter({
  routes
})

// 未登录一律踢到 /login；401 时 request.js 会清 token 并跳这里
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
