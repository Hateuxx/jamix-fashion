import tailwindcss from '@tailwindcss/vite';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const catalogService = process.env.CATALOG_SERVICE_URL ?? 'http://localhost:8080';

export default defineConfig({
  // Root by default (Vercel, local). Behind the gateway it's served under /admin/: build with VITE_BASE_PATH=/admin/.
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [vue(), tailwindcss()],
  server: {
    port: 5174,
    // Fail loudly instead of drifting to another port the docs and gateway don't know about.
    strictPort: true,
    proxy: {
      '/api': catalogService,
      '/img': catalogService,
    },
  },
});
