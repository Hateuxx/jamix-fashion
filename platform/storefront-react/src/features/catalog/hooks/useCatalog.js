import { useCallback, useEffect, useState } from 'react';

import { getCatalog, peekCatalog } from '../api/catalogStore.js';

function initialState(baseUrl) {
  const cached = peekCatalog(baseUrl);
  return cached ? { status: 'success', products: cached, error: null } : { status: 'loading', products: [], error: null };
}

/**
 * The public catalogue, from the shared store (one request, cached 60 s).
 * Returns { status: 'loading' | 'error' | 'success', products, error, reload }.
 */
export function useCatalog(baseUrl) {
  const [state, setState] = useState(() => initialState(baseUrl));
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true; // ignore answers that arrive after unmount (StrictMode, fast navigation)
    getCatalog(baseUrl, { force: attempt > 0 })
      .then((products) => {
        if (!active) return;
        // Same array as before (cache hit): keep the state object so nothing downstream re-renders.
        setState((prev) =>
          prev.status === 'success' && prev.products === products ? prev : { status: 'success', products, error: null },
        );
      })
      .catch((error) => {
        if (active) setState({ status: 'error', products: [], error });
      });
    return () => {
      active = false;
    };
  }, [baseUrl, attempt]);

  const reload = useCallback(() => {
    setState((prev) => ({ ...prev, status: 'loading', error: null }));
    setAttempt((n) => n + 1);
  }, []);

  return { ...state, reload };
}
