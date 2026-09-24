import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

const catalogService = process.env.CATALOG_SERVICE_URL ?? 'http://localhost:8080';

export default defineConfig({
  // Root by default (Vercel, local). Behind the gateway it's served under /tienda/: build with VITE_BASE_PATH=/tienda/.
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // Fail loudly instead of drifting to 5174+ (that port belongs to the admin).
    strictPort: true,
    // Same-origin in dev, exactly like behind the gateway: no CORS, no hard-coded hosts in the bundle.
    proxy: {
      '/api': catalogService,
      '/img': catalogService,
    },
  },
});
