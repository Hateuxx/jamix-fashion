import { useEffect, useMemo, useState } from 'react';

import { CATALOG_API_URL, CATEGORIES, WHATSAPP_NUMBER } from '../config.js';
import { useBag } from '../hooks/useBag.jsx';
import { useProducts } from '../hooks/useProducts.js';
import { DEFAULT_FILTERS, SORTS, applyFilters, filtersFromSearch, filtersToSearch, typesIn } from '../lib/filters.js';
import ProductCard from './ProductCard.jsx';
import QuickView from './QuickView.jsx';

const CATEGORY_SLUGS = CATEGORIES.map((c) => c.slug).filter((s) => s !== 'all');
const PRODUCT_PARAM = 'producto';

const pill = (selected) =>
  `shrink-0 rounded-full border px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-jx-charcoal ${
    selected ? 'border-jx-charcoal bg-jx-charcoal text-jx-white' : 'border-jx-natural bg-jx-white text-jx-charcoal hover:border-jx-charcoal'
  }`;

function SkeletonGrid() {
  return (
    <ul aria-hidden="true" className="grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4">
      {Array.from({ length: 8 }, (_, i) => (
        <li key={i} className="overflow-hidden rounded-2xl bg-jx-white">
          <div className="aspect-[3/4] animate-pulse bg-jx-natural/70" />
          <div className="space-y-2 p-5">
            <div className="h-4 w-2/3 animate-pulse rounded bg-jx-natural/70" />
            <div className="h-4 w-1/3 animate-pulse rounded bg-jx-natural/70" />
          </div>
        </li>
      ))}
    </ul>
  );
}

/**
 * Interactive catalogue backed by GET /api/v1/products (Spring Boot). Fetched once, then filtered,
 * searched and sorted client-side (instant for a few dozen garments). Filters and the open product
 * live in the URL, so any view can be shared or bookmarked.
 */
export default function ProductGrid({ apiBaseUrl = CATALOG_API_URL, whatsappNumber = WHATSAPP_NUMBER }) {
  const { status, products, error, reload } = useProducts(apiBaseUrl);
  const bag = useBag();
  const [filters, setFilters] = useState(() => filtersFromSearch(window.location.search, CATEGORY_SLUGS));
  const [openSlug, setOpenSlug] = useState(() => new URLSearchParams(window.location.search).get(PRODUCT_PARAM));

  const { sync } = bag;
  // Refresh prices of a bag saved in an earlier visit against the live catalogue.
  useEffect(() => {
    if (status === 'success') sync(products);
  }, [status, products, sync]);

  useEffect(() => {
    const params = new URLSearchParams(filtersToSearch(filters, window.location.search));
    if (openSlug) params.set(PRODUCT_PARAM, openSlug);
    else params.delete(PRODUCT_PARAM);
    const search = params.toString();
    window.history.replaceState(null, '', `${window.location.pathname}${search ? `?${search}` : ''}`);
  }, [filters, openSlug]);

  const update = (patch) => setFilters((f) => ({ ...f, ...patch }));

  function selectCategory(category) {
    setFilters((f) => ({
      ...f,
      category,
      // Keep the type only if it exists in the new category.
      type: typesIn(products, category).includes(f.type) ? f.type : 'all',
    }));
  }

  const counts = useMemo(() => {
    const result = { all: products.length };
    for (const p of products) result[p.category] = (result[p.category] ?? 0) + 1;
    return result;
  }, [products]);

  const types = useMemo(() => typesIn(products, filters.category), [products, filters.category]);
  const visible = useMemo(() => applyFilters(products, filters), [products, filters]);
  const openProduct = openSlug ? products.find((p) => p.slug === openSlug) : null;
  const filtered = JSON.stringify(filters) !== JSON.stringify(DEFAULT_FILTERS);

  return (
    <section aria-labelledby="catalog-title" className="mx-auto max-w-7xl px-4 pb-28 pt-12 sm:px-6 sm:pt-16">
      <div className="mb-8 flex flex-col gap-2">
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-jx-gold-dark">Colección permanente</p>
        <h2 id="catalog-title" className="font-display text-3xl font-semibold sm:text-4xl">
          Arma el look de tu familia
        </h2>
        <p className="max-w-2xl text-sm text-jx-charcoal-soft">
          Elige cada prenda, para quién es y su talla. Al final envías el look completo en un solo WhatsApp, con el
          precio mayorista aplicado si llegas a 6 unidades de una misma prenda.
        </p>
      </div>

      {/* Filters */}
      <div className="mb-8 flex flex-col gap-4 rounded-2xl bg-jx-white p-4 sm:p-5">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div role="group" aria-label="Filtrar por categoría" className="flex gap-2 overflow-x-auto pb-1">
            {CATEGORIES.map(({ slug, label }) => (
              <button key={slug} type="button" aria-pressed={filters.category === slug}
                onClick={() => selectCategory(slug)} className={pill(filters.category === slug)}>
                {label}
                {counts[slug] != null && <span className="ml-1.5 opacity-60">{counts[slug]}</span>}
              </button>
            ))}
          </div>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <label className="relative flex-1 sm:w-64 sm:flex-none">
              <span className="sr-only">Buscar prendas</span>
              <input
                type="search"
                value={filters.query}
                onChange={(e) => update({ query: e.target.value })}
                placeholder="Buscar: vestido, top, niña…"
                className="w-full rounded-full border border-jx-natural px-4 py-2 text-sm outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige"
              />
            </label>
            <label className="flex items-center gap-2 text-sm">
              <span className="sr-only sm:not-sr-only">Ordenar</span>
              <select value={filters.sort} onChange={(e) => update({ sort: e.target.value })}
                className="flex-1 rounded-full border border-jx-natural bg-jx-white px-4 py-2 outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige">
                {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </label>
          </div>
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div role="group" aria-label="Filtrar por tipo de prenda" className="flex gap-2 overflow-x-auto pb-1">
            <button type="button" aria-pressed={filters.type === 'all'} onClick={() => update({ type: 'all' })}
              className={pill(filters.type === 'all')}>
              Todos los tipos
            </button>
            {types.map((t) => (
              <button key={t} type="button" aria-pressed={filters.type === t} onClick={() => update({ type: t })}
                className={pill(filters.type === t)}>
                {t}
              </button>
            ))}
          </div>
          <label className="flex shrink-0 cursor-pointer items-center gap-2 text-sm">
            <input type="checkbox" checked={filters.onlyAvailable} onChange={(e) => update({ onlyAvailable: e.target.checked })}
              className="h-4 w-4 accent-jx-charcoal" />
            Solo disponibles
          </label>
        </div>
      </div>

      <div aria-live="polite" aria-busy={status === 'loading'}>
        {status === 'loading' && <SkeletonGrid />}

        {status === 'error' && (
          <div role="alert" className="flex flex-col items-start gap-4 rounded-2xl bg-jx-white p-8">
            <p className="font-display text-xl">No pudimos cargar el catálogo.</p>
            <p className="text-sm text-jx-muted">{error?.message}</p>
            <button type="button" onClick={reload}
              className="rounded-full bg-jx-charcoal px-5 py-2.5 text-sm font-medium text-jx-white hover:bg-jx-charcoal-soft">
              Reintentar
            </button>
          </div>
        )}

        {status === 'success' && (
          <>
            <div className="mb-4 flex items-center justify-between text-sm text-jx-muted">
              <p>{visible.length} {visible.length === 1 ? 'prenda' : 'prendas'}</p>
              {filtered && (
                <button type="button" onClick={() => setFilters(DEFAULT_FILTERS)}
                  className="underline underline-offset-4 hover:text-jx-charcoal">
                  Limpiar filtros
                </button>
              )}
            </div>

            {visible.length === 0 ? (
              <div className="rounded-2xl bg-jx-white p-8 text-center">
                <p className="font-display text-xl">No encontramos prendas con esos filtros.</p>
                <button type="button" onClick={() => setFilters(DEFAULT_FILTERS)}
                  className="mt-4 rounded-full border border-jx-charcoal px-5 py-2 text-sm font-medium hover:bg-jx-crudo">
                  Ver todo el catálogo
                </button>
              </div>
            ) : (
              // Keyed by the filters so the staggered fade-in replays on every change.
              <ul key={JSON.stringify(filters)} className="stagger-children grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4">
                {visible.map((product) => (
                  <ProductCard key={product.id} product={product} apiBaseUrl={apiBaseUrl}
                    onOpen={() => setOpenSlug(product.slug)} />
                ))}
              </ul>
            )}
          </>
        )}
      </div>

      {openProduct && (
        <QuickView
          key={openProduct.slug}
          product={openProduct}
          apiBaseUrl={apiBaseUrl}
          whatsappNumber={whatsappNumber}
          onClose={() => setOpenSlug(null)}
        />
      )}
    </section>
  );
}
