import { useCallback, useEffect, useMemo, useState } from 'react';

import { CATEGORIES } from '../../../shared/config.js';
import { replaceSearch } from '../../../shared/url.js';
import { DEFAULT_FILTERS, applyFilters, filtersFromSearch, filtersToSearch, typesIn } from '../lib/filters.js';

const CATEGORY_SLUGS = CATEGORIES.map((c) => c.slug).filter((s) => s !== 'all');

/**
 * Filter state for the catalogue, mirrored in the URL (?categoria=…&tipo=…&q=…) so any view can be
 * shared. Returns derived data plus STABLE actions, so memoized children don't re-render for nothing.
 */
export function useCatalogFilters(products) {
  const [filters, setFilters] = useState(() => filtersFromSearch(window.location.search, CATEGORY_SLUGS));

  useEffect(() => {
    replaceSearch(filtersToSearch(filters, window.location.search));
  }, [filters]);

  const update = useCallback((patch) => setFilters((f) => ({ ...f, ...patch })), []);
  const reset = useCallback(() => setFilters(DEFAULT_FILTERS), []);
  const selectCategory = useCallback(
    (category) =>
      setFilters((f) => ({
        ...f,
        category,
        // Keep the garment type only if it exists in the new category.
        type: typesIn(products, category).includes(f.type) ? f.type : 'all',
      })),
    [products],
  );

  const counts = useMemo(() => {
    const result = { all: products.length };
    for (const p of products) result[p.category] = (result[p.category] ?? 0) + 1;
    return result;
  }, [products]);
  const types = useMemo(() => typesIn(products, filters.category), [products, filters.category]);
  const visible = useMemo(() => applyFilters(products, filters), [products, filters]);
  const isFiltered = useMemo(
    () => Object.keys(DEFAULT_FILTERS).some((key) => filters[key] !== DEFAULT_FILTERS[key]),
    [filters],
  );

  return { filters, update, reset, selectCategory, counts, types, visible, isFiltered };
}
