export default function ProductCard({ product, apiBaseUrl, onOpen }) {
  const { slug, name, categoryLabel, type, priceLabel, wholesaleLabel, wholesaleMinUnits, imageUrl, gallery, inStock } =
    product;
  const titleId = `product-${slug}`;
  const photoCount = (imageUrl ? 1 : 0) + (gallery?.length ?? 0);

  return (
    <li className="group hover-lift flex flex-col overflow-hidden rounded-2xl bg-jx-white motion-safe:animate-fade-in">
      <article aria-labelledby={titleId} className="flex h-full flex-col">
        <button
          type="button"
          onClick={onOpen}
          aria-label={`Ver ${name}`}
          className="relative aspect-[3/4] overflow-hidden bg-jx-natural text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-jx-charcoal"
        >
          {imageUrl ? (
            <img
              src={`${apiBaseUrl}${imageUrl}`}
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
          <p className="text-base font-semibold">{priceLabel}</p>
          {wholesaleLabel && (
            <p className="mb-3 text-xs text-jx-muted">
              Mayorista {wholesaleLabel} · desde {wholesaleMinUnits} unid.
            </p>
          )}
          <button
            type="button"
            onClick={onOpen}
            className={
              inStock
                ? 'mt-auto inline-flex items-center justify-center rounded-full bg-jx-charcoal px-4 py-2.5 text-sm font-medium text-jx-white transition-colors hover:bg-jx-charcoal-soft'
                : 'mt-auto inline-flex items-center justify-center rounded-full border border-jx-charcoal px-4 py-2.5 text-sm font-medium transition-colors hover:bg-jx-crudo'
            }
          >
            {inStock ? 'Agregar al look' : 'Ver y avisarme'}
          </button>
        </div>
      </article>
    </li>
  );
}
