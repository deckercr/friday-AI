import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/auth': 'http://localhost:8081',
      '/api': 'http://localhost:8081',
      '/ws': {
        target: 'http://localhost:8081',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
