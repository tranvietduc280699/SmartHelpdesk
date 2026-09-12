import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      /*
       * Backend chặn CORS: SecurityConfig gọi .cors(...::disable) và không
       * khai báo CorsConfigurationSource nào, nên trình duyệt gọi thẳng sang
       * cổng 8080 sẽ hỏng ngay ở preflight. Đi vòng qua proxy của Vite thì
       * request cùng origin với trang, không phát sinh preflight.
       *
       * Bỏ proxy này được khi backend bật CORS cho origin của FE.
       */
      proxy: {
        '/api': {
          target: env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    preview: {
      proxy: {
        '/api': {
          target: env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      include: ['src/**/*.{test,spec}.ts'],
    },
  }
})
