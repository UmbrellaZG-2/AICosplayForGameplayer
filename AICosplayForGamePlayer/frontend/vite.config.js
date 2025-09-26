import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath, URL } from 'node:url'

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
        resolve(__dirname, '../../image/')
      ]
    }
  },
  // 配置路径别名
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '/Character': fileURLToPath(new URL('./resource/Character', import.meta.url))
    }
  },
  // 禁用默认的publicDir配置
  publicDir: false
})