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
    },
    client:{
        overlay:{
            runtimeErrors:(error)=>{
                const message = error.message || '';
                if(message.includes('ResizeObserver loop completed with undelivered notifications')){
                    return false;
                }
                return true;
            }
        }
    }
  }
}
