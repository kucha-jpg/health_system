import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:9090'

export default defineConfig({
  plugins: [
    vue(),
    Components({
      dts: false,
      resolvers: [ElementPlusResolver({ importStyle: 'css' })]
    })
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          if (id.includes('element-plus')) {
            const epCompMatch = id.match(/element-plus\/es\/components\/([^/]+)\//)
            if (epCompMatch) {
              return `vendor-ep-${epCompMatch[1]}`
            }
            return 'vendor-element-plus-core'
          }
          if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/pinia/')) {
            return 'vendor-vue-core'
          }
          if (id.includes('/axios/')) {
            return 'vendor-axios'
          }
          if (id.includes('/echarts/')) {
            return 'vendor-echarts'
          }
          return 'vendor-misc'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
})
