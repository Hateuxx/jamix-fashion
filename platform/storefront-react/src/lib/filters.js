// Pure catalogue filtering: category, garment type, text search, availability and sort.

export const SORTS = [
  { value: 'relevance', label: 'Destacados' },
  { value: 'price-asc', label: 'Precio: menor a mayor' },
  { value: 'price-desc', label: 'Precio: mayor a menor' },
  { value: 'name', label: 'Nombre (A–Z)' },
];

export const DEFAULT_FILTERS = { category: 'all', type: 'all', query: '', sort: 'relevance', onlyAvailable: false };

/** Lower-case and accent-free, so "tunica" finds "Túnica" and "NINA" finds "Niña". */
export const normalize = (text) =>
  (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

/** Garment types present in a category, in catalogue order (for the type chips). */
export function typesIn(products, category) {
  const types = [];
  for (const p of products) {
    if ((category === 'all' || p.category === category) && p.type && !types.includes(p.type)) types.push(p.type);
  }
  return types;
}

export function applyFilters(products, filters) {
  const words = normalize(filters.query).split(/\s+/).filter(Boolean);
  const result = products.filter((p) => {
    if (filters.category !== 'all' && p.category !== filters.category) return false;
    if (filters.type !== 'all' && p.type !== filters.type) return false;
    if (filters.onlyAvailable && !p.inStock) return false;
    if (words.length === 0) return true;
    const haystack = normalize(`${p.name} ${p.type ?? ''} ${p.categoryLabel ?? ''} ${p.description ?? ''}`);
    return words.every((w) => haystack.includes(w));
  });

  const byPrice = (a, b) => Number(a.price) - Number(b.price);
  switch (filters.sort) {
    case 'price-asc':
      return [...result].sort(byPrice);
    case 'price-desc':
      return [...result].sort((a, b) => byPrice(b, a));
    case 'name':
      return [...result].sort((a, b) => a.name.localeCompare(b.name, 'es'));
    default:
      return result; // API order: grouped by category
  }
}

const PARAMS = { category: 'categoria', type: 'tipo', query: 'q', sort: 'orden', onlyAvailable: 'disponibles' };

/** Reads filters from the URL (same ?categoria= links as the SSR page). */
export function filtersFromSearch(search, validCategories) {
  const params = new URLSearchParams(search);
  const category = params.get(PARAMS.category);
  const sort = params.get(PARAMS.sort);
  return {
    category: validCategories.includes(category) ? category : 'all',
    type: params.get(PARAMS.type) || 'all',
    query: params.get(PARAMS.query) ?? '',
    sort: SORTS.some((s) => s.value === sort) ? sort : 'relevance',
    onlyAvailable: params.get(PARAMS.onlyAvailable) === '1',
  };
}

/** Writes non-default filters into the URL so a filtered view can be shared. */
export function filtersToSearch(filters, currentSearch = '') {
  const params = new URLSearchParams(currentSearch);
  for (const [key, param] of Object.entries(PARAMS)) {
    const value = filters[key];
    const isDefault = value === DEFAULT_FILTERS[key];
    if (isDefault) params.delete(param);
    else params.set(param, key === 'onlyAvailable' ? '1' : String(value).trim());
  }
  const text = params.toString();
  return text ? `?${text}` : '';
}
