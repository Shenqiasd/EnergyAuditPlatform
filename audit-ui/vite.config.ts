import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  // SpreadJS is loaded via CDN (index.html) and exposed on window.GC.
  // Externalising these names prevents Vite from bundling them and
  // ensures the CDN globals are resolved at runtime.
  build: {
    rollupOptions: {
      external: [
        '@grapecity/spread-sheets',
        '@grapecity/spread-sheets-designer',
        '@grapecity/spread-sheets-designer-resources-en',
        '@grapecity/spread-excelio',
      ],
    },
  },
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5000,
    allowedHosts: true, // Required for Replit proxied preview (all Host headers must be allowed)
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
