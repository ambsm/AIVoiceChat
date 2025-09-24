const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8081,
    open: true,
    proxy: {
      '/ai': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/ai': '/ai'
        }
      }
    }
  },
  configureWebpack: {
    resolve: {
      alias: {
        '@': require('path').resolve(__dirname, 'src')
      }
    }
  }
})