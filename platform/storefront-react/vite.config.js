import { cpSync, existsSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

const catalogService = process.env.CATALOG_SERVICE_URL ?? 'http://localhost:8080';

// WebP photo variants generated in catalog-service (npm run images there; see src/shared/photos.js).
const optimizedPhotos = fileURLToPath(new URL('../catalog-service/src/main/resources/static/img/opt', import.meta.url));

/**
 * Ships the WebP variants inside dist/img/opt, so the storefront's CDN (Vercel) serves the photos
 * instead of the catalog instance. In dev they come through the /img proxy below.
 */
function shipOptimizedPhotos() {
  return {
    name: 'jamix-optimized-photos',
    apply: 'build',
    writeBundle({ dir }) {
      if (!existsSync(optimizedPhotos)) {
        // Building without the catalog-service folder next to it: <Photo> falls back to the originals.
        this.warn(`No se encontraron las fotos optimizadas en ${optimizedPhotos}; se usarán las originales.`);
        return;
      }
      cpSync(optimizedPhotos, join(dir, 'img/opt'), { recursive: true });
    },
  };
}

export default defineConfig({
  // Root by default (Vercel, local). Behind the gateway it's served under /tienda/: build with VITE_BASE_PATH=/tienda/.
  base: process.env.VITE_BASE_PATH || '/',
  plugins: [react(), tailwindcss(), shipOptimizedPhotos()],
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
