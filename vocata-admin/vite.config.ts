import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 根据当前模式加载对应的环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      port: 3001,
      host: true,
      proxy: {
        // 代理所有 /api 开头的请求到后端服务器
        '/api': {
          target: env.VITE_APP_URL,
          changeOrigin: true,
          secure: false,
        }
      }
    },
  }
})
