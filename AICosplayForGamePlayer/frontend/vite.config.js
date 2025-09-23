import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        withCredentials: true
      }
    },
    // 配置静态资源访问
    fs: {
      allow: [
        '.',
        '/d:/Code/AICosplaying/AICosplayForGamePlayer/image/'
      ]
    }
  },
  // 配置路径别名
  resolve: {
    alias: {
      '/image/Character': resolve('/d:/Code/AICosplaying/AICosplayForGamePlayer/image/Character/')
    }
  }
})