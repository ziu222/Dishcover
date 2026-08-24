import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Dev proxy: frontend gọi path tương đối `/api/...`, proxy chuyển sang API Gateway (8080)
// và cắt tiền tố `/api`. Tránh CORS mà không phải sửa Gateway.
// ponytail: proxy chỉ cho dev — production cần CORS ở Gateway hoặc reverse-proxy chung origin.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ''),
      },
    },
  },
})
