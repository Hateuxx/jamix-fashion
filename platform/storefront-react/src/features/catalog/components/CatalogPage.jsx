import { useEffect } from 'react';

import { CATALOG_API_URL, WHATSAPP_NUMBER } from '../../../shared/config.js';
import { useBagActions } from '../../bag/BagProvider.jsx';
import { useCatalog } from '../hooks/useCatalog.js';
import { useCatalogFilters } from '../hooks/useCatalogFilters.js';
import { useOpenProduct } from '../hooks/useOpenProduct.js';
import CatalogFilters from './CatalogFilters.jsx';
import ProductGrid from './ProductGrid.jsx';
import QuickView from './QuickView.jsx';
import SkeletonGrid from './SkeletonGrid.jsx';

/**
 * Catalogue page (container): wires data, filters, URL state and the bag together and hands
 * stable props to presentational components. It reads only the bag's ACTIONS, so opening the
 * drawer or showing a toast never re-renders the page or its 35 animated cards.
 */
export default function CatalogPage({ apiBaseUrl = CATALOG_API_URL, whatsappNumber = WHATSAPP_NUMBER }) {
  const { status, products, error, reload } = useCatalog(apiBaseUrl);
  const { filters, update, reset, selectCategory, counts, types, visible, isFiltered } = useCatalogFilters(products);
  const { openSlug, open, close } = useOpenProduct();
  const { sync } = useBagActions();

  // Refresh prices of a bag saved in an earlier visit against the live catalogue.
  useEffect(() => {
    if (status === 'success') sync(products);
  }, [status, products, sync]);

  const openProduct = openSlug ? products.find((p) => p.slug === openSlug) : null;

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

      <CatalogFilters filters={filters} counts={counts} types={types} onUpdate={update} onSelectCategory={selectCategory} />

      <div aria-live="polite" aria-busy={status === 'loading'}>
        {status === 'loading' && <SkeletonGrid />}

        {status === 'error' && (
          <div role="alert" className="flex flex-col items-start gap-4 rounded-2xl bg-jx-white p-8">
            <p className="font-display text-xl">No pudimos cargar el catálogo.</p>
            <p className="text-sm text-jx-muted">{error?.message}</p>
            <button
              type="button"
              onClick={reload}
              className="rounded-full bg-jx-charcoal px-5 py-2.5 text-sm font-medium text-jx-white hover:bg-jx-charcoal-soft"
            >
              Reintentar
            </button>
          </div>
        )}

        {status === 'success' && (
          <>
            <div className="mb-4 flex items-center justify-between text-sm text-jx-muted">
              <p>
                {visible.length} {visible.length === 1 ? 'prenda' : 'prendas'}
              </p>
              {isFiltered && (
                <button type="button" onClick={reset} className="underline underline-offset-4 hover:text-jx-charcoal">
                  Limpiar filtros
                </button>
              )}
            </div>

            {visible.length === 0 ? (
              <div className="rounded-2xl bg-jx-white p-8 text-center">
                <p className="font-display text-xl">No encontramos prendas con esos filtros.</p>
                <button
                  type="button"
                  onClick={reset}
                  className="mt-4 rounded-full border border-jx-charcoal px-5 py-2 text-sm font-medium hover:bg-jx-crudo"
                >
                  Ver todo el catálogo
                </button>
              </div>
            ) : (
              <ProductGrid products={visible} apiBaseUrl={apiBaseUrl} onOpen={open} />
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
          onClose={close}
        />
      )}
    </section>
  );
}
