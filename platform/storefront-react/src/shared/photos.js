// Responsive photo sources. The catalogue API returns originals ("/img/falda-bobo.jpeg"); their WebP
// variants are generated in catalog-service (npm run images) and shipped inside this build (see
// vite.config.js), so the CDN that serves the storefront also serves the photos.

/** Keep in sync with WIDTHS in catalog-service/scripts/optimize-images.mjs. */
export const OPTIMIZED_WIDTHS = [480, 960];

const LOCAL_PHOTO = /^\/img\/([^/]+)\.(jpe?g|png)$/i;

/**
 * { src, srcSet }: src is always the original (the fallback); srcSet is only set for local photos.
 * assetBase is where this build is served from ("/" on Vercel, "/tienda/" behind the gateway).
 */
export function photoSources(imageUrl, { apiBaseUrl = '', assetBase = '/' } = {}) {
  const src = `${apiBaseUrl}${imageUrl}`;
  const local = LOCAL_PHOTO.exec(imageUrl ?? '');
  if (!local) return { src, srcSet: undefined };
  const base = assetBase.endsWith('/') ? assetBase : `${assetBase}/`;
  const srcSet = OPTIMIZED_WIDTHS.map((w) => `${base}img/opt/${local[1]}-${w}.webp ${w}w`).join(', ');
  return { src, srcSet };
}
