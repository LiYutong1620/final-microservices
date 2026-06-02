module.exports = {
  devServer: {
    port: 80,
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 指向 Spring Cloud Gateway 统一反向代理
        changeOrigin: true,
        pathRewrite: {
          '^/api': '/api'
        }
      }
    }
  }
}
