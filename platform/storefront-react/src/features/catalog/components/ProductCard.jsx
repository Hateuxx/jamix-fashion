import { m } from 'motion/react';
import { memo } from 'react';

import Photo from '../../../shared/Photo.jsx';

const EASE_OUT = [0.22, 1, 0.36, 1];

/**
 * Two layers on purpose:
 *  - <m.li>: Motion owns its `transform` (enter/exit + gliding to a new grid cell when filters change).
 *  - <article class="hover-lift">: the CSS hover lift owns ITS transform.
 * On the same element Motion's inline transform would override the CSS hover and kill it.
 */
function ProductCard({ product, apiBaseUrl, onOpen, index = 0, ref }) {
  const { slug, name, categoryLabel, type, priceLabel, imageUrl, gallery, inStock } = product;
  const titleId = `product-${slug}`;
  const handleOpen = () => onOpen(slug);
  const photoCount = (imageUrl ? 1 : 0) + (gallery?.length ?? 0);

  return (
    <m.li
      ref={ref}
      layout="position" // glide to the new cell; "position" never stretches text or photos
      initial={{ opacity: 0, y: 16, scale: 0.98 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, scale: 0.94, transition: { duration: 0.18 } }}
      transition={{
        duration: 0.45,
        ease: EASE_OUT,
        delay: Math.min(index, 8) * 0.04, // stagger the first rows, capped so long lists don't lag
        layout: { type: 'spring', stiffness: 380, damping: 34 },
      }}
      className="flex"
    >
      <article
        aria-labelledby={titleId}
        className="group hover-lift flex w-full flex-col overflow-hidden rounded-2xl bg-jx-white"
      >
        <button
          type="button"
          onClick={handleOpen}
          aria-label={`Ver ${name}`}
          className="relative aspect-[3/4] overflow-hidden bg-jx-natural text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-jx-charcoal"
        >
          {imageUrl ? (
            <Photo
              imageUrl={imageUrl}
              apiBaseUrl={apiBaseUrl}
              sizes="(min-width: 1280px) 18rem, (min-width: 1024px) 25vw, 50vw"
              alt=""
              loading="lazy"
              decoding="async"
              className="h-full w-full object-cover transition-transform duration-700 ease-out motion-safe:group-hover:scale-105"
            />
          ) : (
            <span className="flex h-full items-center justify-center p-4 text-center font-display text-lg text-jx-charcoal-soft">
              Foto próximamente
            </span>
          )}
          <span className="absolute left-3 top-3 rounded-full bg-jx-white/90 px-3 py-1 text-xs font-medium">
            {categoryLabel}
          </span>
          {!inStock && (
            <span className="absolute right-3 top-3 rounded-full bg-jx-charcoal px-3 py-1 text-xs font-medium text-jx-white">
              Agotado
            </span>
          )}
          {photoCount > 1 && (
            <span className="absolute bottom-3 right-3 rounded-full bg-jx-charcoal/75 px-2.5 py-1 text-xs text-jx-white">
              {photoCount} fotos
            </span>
          )}
        </button>

        <div className="flex flex-1 flex-col gap-1 p-4 sm:p-5">
          {type && <p className="text-[0.7rem] font-medium uppercase tracking-wider text-jx-gold-dark">{type}</p>}
          <h3 id={titleId} className="font-display text-lg font-semibold leading-tight">
            {name}
          </h3>
          <p className="mb-3 text-base font-semibold">{priceLabel}</p>
          <m.button
            type="button"
            onClick={handleOpen}
            whileTap={{ scale: 0.96 }} // tactile press feedback, especially on phones
            className={
              inStock
                ? 'mt-auto inline-flex items-center justify-center rounded-full bg-jx-charcoal px-4 py-2.5 text-sm font-medium text-jx-white transition-colors hover:bg-jx-charcoal-soft'
                : 'mt-auto inline-flex items-center justify-center rounded-full border border-jx-charcoal px-4 py-2.5 text-sm font-medium transition-colors hover:bg-jx-crudo'
            }
          >
            {inStock ? 'Agregar al look' : 'Ver y avisarme'}
          </m.button>
        </div>
      </article>
    </m.li>
  );
}

// Memoized: the grid passes stable props (same product object, shared onOpen), so a card only
// re-renders when its own data or grid position changes, not on every bag or dialog update.
export default memo(ProductCard);
