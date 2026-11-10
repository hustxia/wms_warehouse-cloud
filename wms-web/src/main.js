import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'
import router from './router'
import store from './store'

Vue.use(ElementUI, { size: 'small' })
Vue.config.productionTip = false

// V6.4 按钮级权限：v-hasPermi="['demo_fee:add']"，命中任一即显示；超管 *:*:* 直通
Vue.directive('hasPermi', {
  inserted(el, binding) {
    let perms = []
    try { perms = JSON.parse(localStorage.getItem('perms') || '[]') } catch (e) { perms = [] }
    const need = binding.value || []
    const ok = perms.includes('*:*:*') || need.some(p => perms.includes(p)
      || perms.includes(p.replace(/:[^:]*$/, ':*')))
    if (!ok) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
