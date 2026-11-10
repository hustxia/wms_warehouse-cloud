const { defineConfig } = require('@vue/cli-service')

// 代理目标必须和 .env.development 的 VUE_APP_PROXY_TARGET 一致，且指向 cloud 网关 8100。
// 历史坑：.env 里曾写成单体端口 8140，但这里硬编码 8100 —— 两处不一致时按 8140
// 排查会白费功夫；改成单一真源后，切换单体/微服务只改 .env 一处。
const PROXY_TARGET = process.env.VUE_APP_PROXY_TARGET || 'http://127.0.0.1:8100'

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 9040,
    // 缺了它，前端 history 路由刷新页面会 404
    historyApiFallback: true,
    proxy: {
      '/dev-api': {
        target: PROXY_TARGET,
        changeOrigin: true,
        // http-proxy-middleware 多条 pathRewrite 不保证顺序执行，合并成一条正则
        pathRewrite: { '^/dev-api(/api/demo)?': '' }
      },
      // 静态素材（后端 resources/static/img），漏配会整页显示占位图
      '/img': {
        target: PROXY_TARGET,
        changeOrigin: true
      }
    }
  }
})
