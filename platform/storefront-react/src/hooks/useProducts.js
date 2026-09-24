import { useCallback, useEffect, useState } from 'react';

/**
 * Loads the public catalogue from the Spring Boot API.
 * Returns { status: 'loading' | 'error' | 'success', products, error, reload }.
 */
export function useProducts(apiBaseUrl) {
  const [state, setState] = useState({ status: 'loading', products: [], error: null });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setState((prev) => ({ ...prev, status: 'loading', error: null }));

    fetch(`${apiBaseUrl}/api/v1/products`, {
      headers: { Accept: 'application/json' },
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(`El catálogo respondió ${res.status}`);
        const products = await res.json();
        setState({ status: 'success', products, error: null });
      })
      .catch((error) => {
        // Unmounts, retries and StrictMode's double effect abort the previous request; that's not a failure.
        if (error.name === 'AbortError') return;
        setState({ status: 'error', products: [], error });
      });

    return () => controller.abort();
  }, [apiBaseUrl, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);

  return { ...state, reload };
}
