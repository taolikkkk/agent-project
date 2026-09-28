import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import ElementPlus from 'unplugin-element-plus/vite'

export default defineConfig({
  plugins: [vue(), Components({ resolvers: [ElementPlusResolver()], dts: false }), ElementPlus({})],
  server: {
    port: 5173,
    proxy: Object.fromEntries(['/auth', '/api', '/chat/'].map(path => [path, {
      target: process.env.BACKEND_URL || 'http://127.0.0.1:8009',
      changeOrigin: true,
      timeout: 600000,
      proxyTimeout: 600000,
    }])),
  },
})
