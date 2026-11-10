import axios from 'axios'
import { Message } from 'element-ui'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 0 && res.code !== 200) {
      Message({ message: res.msg || '请求失败', type: 'error', duration: 5000 })
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  error => {
    const resp = error.response
    if (!resp) {
      // 网络层失败：没有 HTTP 响应（后端/网关没起、代理端口不通）。
      // 过去这里只显示 axios 的 "Network Error"，用户无法判断是服务挂了还是接口坏了。
      // 会话结束后后台服务被回收是最常见原因，直接给出可执行的恢复命令。
      Message({
        message: '无法连接后端网关（8100）：请在终端运行 bash scripts/cloud-up.sh --with-web 后刷新',
        type: 'error',
        duration: 8000
      })
      return Promise.reject(error)
    }
    if (resp.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/#/login'
      return Promise.reject(new Error('登录状态已失效'))
    }
    Message({ message: (resp.data && resp.data.msg) || error.message, type: 'error', duration: 5000 })
    return Promise.reject(error)
  }
)

// MyBatis-Plus Page {records,total} -> {list,total}
export function toPageData(res) {
  const page = res.data || {}
  return {
    code: res.code,
    msg: res.msg,
    data: { list: page.records || [], total: page.total || 0 }
  }
}

export default service
