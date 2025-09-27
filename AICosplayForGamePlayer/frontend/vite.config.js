import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  // 配置插件
  plugins: [
    vue(),
    // 自定义插件处理资源路径
    {
      name: 'resource-alias',
      configureServer(server) {
        // 添加中间件处理/Character路径请求
        server.middlewares.use('/Character', (req, res, next) => {
          // 不需要额外处理，因为publicDir已经设置为'resource'
          // 直接让Vite处理请求
          next();
        });
      }
    }
  ],
  
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
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  
  // 配置静态资源目录
  publicDir: 'resource'
})