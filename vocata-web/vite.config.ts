import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
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
    proxy: {
      '/client': {
        target: 'http://101.200.141.46:9009',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/client/, '')
      }, '/api': {
        target: 'http://101.200.141.46:9009',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/client/, '')
      }
    }
  },
})
