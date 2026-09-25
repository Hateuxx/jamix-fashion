// Catalogue data shared by every component that needs it (grid today; product page, recommendations,
// bag price sync tomorrow). Plain JS, no React, so it's unit-tested with `node --test`.
//
//   - Dedupe: components asking at the same time share ONE in-flight request.
//   - Freshness: data is reused for CATALOG_TTL_MS, the same max-age the backend sends
//     (Cache-Control: public, max-age=60), so mounting another view doesn't hit the API again.
//
// When the app grows several endpoints with mutations (orders, reviews, pagination), this is the
// point to adopt TanStack Query: same idea, plus background refetch, retries and devtools.

export const CATALOG_TTL_MS = 60_000;

/** baseUrl -> { data?, fetchedAt?, promise? } */
const entries = new Map();

export async function fetchProducts(baseUrl, { fetchImpl = fetch } = {}) {
  const res = await fetchImpl(`${baseUrl}/api/v1/products`, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`El catálogo respondió ${res.status}`);
  return res.json();
}

/** Fresh cached data, or undefined. Lets a view render instantly instead of flashing a skeleton. */
export function peekCatalog(baseUrl, now = Date.now()) {
  const entry = entries.get(baseUrl);
  return entry?.data && now - entry.fetchedAt < CATALOG_TTL_MS ? entry.data : undefined;
}

/**
 * The catalogue for this API. `force` skips fresh cached data (the "Reintentar" button), but a
 * request already in flight is always shared, even when forced.
 */
export function getCatalog(baseUrl, { force = false, now = Date.now(), fetchImpl } = {}) {
  const entry = entries.get(baseUrl);
  if (entry?.promise) return entry.promise;
  const fresh = peekCatalog(baseUrl, now);
  if (fresh && !force) return Promise.resolve(fresh);

  const promise = fetchProducts(baseUrl, { fetchImpl })
    .then((data) => {
      entries.set(baseUrl, { data, fetchedAt: Date.now() });
      return data;
    })
    .catch((error) => {
      // Keep older data (if any) so a failed refresh doesn't wipe a working page; drop the promise.
      const previous = entries.get(baseUrl);
      if (previous?.data) entries.set(baseUrl, { data: previous.data, fetchedAt: previous.fetchedAt });
      else entries.delete(baseUrl);
      throw error;
    });
  entries.set(baseUrl, { ...entry, promise });
  return promise;
}

/** Tests only. */
export function clearCatalogCache() {
  entries.clear();
}
