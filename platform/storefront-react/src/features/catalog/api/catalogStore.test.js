import assert from 'node:assert/strict';
import { beforeEach, describe, it } from 'node:test';

import { CATALOG_TTL_MS, clearCatalogCache, getCatalog, peekCatalog } from './catalogStore.js';

const API = 'https://api.test';
const products = [{ id: 1, slug: 'falda-bobo' }];

/** A fake fetch that counts calls and can be made to fail. */
function fakeFetch({ fail = false, delay = 5 } = {}) {
  const calls = [];
  const impl = async (url) => {
    calls.push(url);
    await new Promise((r) => setTimeout(r, delay));
    if (fail) return { ok: false, status: 503, json: async () => ({}) };
    return { ok: true, status: 200, json: async () => products };
  };
  return { impl, calls };
}

describe('catalogStore', () => {
  beforeEach(() => clearCatalogCache());

  it('shares one request between callers that ask at the same time', async () => {
    const { impl, calls } = fakeFetch();
    const [a, b, c] = await Promise.all([
      getCatalog(API, { fetchImpl: impl }),
      getCatalog(API, { fetchImpl: impl }),
      getCatalog(API, { fetchImpl: impl, force: true }), // even forced: the in-flight one is reused
    ]);
    assert.equal(calls.length, 1);
    assert.equal(a, b);
    assert.equal(b, c);
  });

  it('reuses fresh data and refetches after the TTL or when forced', async () => {
    const { impl, calls } = fakeFetch();
    await getCatalog(API, { fetchImpl: impl });
    await getCatalog(API, { fetchImpl: impl });
    assert.equal(calls.length, 1);
    assert.deepEqual(peekCatalog(API), products);

    await getCatalog(API, { fetchImpl: impl, now: Date.now() + CATALOG_TTL_MS + 1 });
    assert.equal(calls.length, 2);
    await getCatalog(API, { fetchImpl: impl, force: true });
    assert.equal(calls.length, 3);
  });

  it('does not cache errors, and a later call can succeed', async () => {
    const failing = fakeFetch({ fail: true });
    await assert.rejects(getCatalog(API, { fetchImpl: failing.impl }), /503/);
    assert.equal(peekCatalog(API), undefined);

    const ok = fakeFetch();
    assert.deepEqual(await getCatalog(API, { fetchImpl: ok.impl }), products);
  });

  it('keeps previous data when a forced refresh fails', async () => {
    await getCatalog(API, { fetchImpl: fakeFetch().impl });
    await assert.rejects(getCatalog(API, { fetchImpl: fakeFetch({ fail: true }).impl, force: true }));
    assert.deepEqual(peekCatalog(API), products);
  });
});
