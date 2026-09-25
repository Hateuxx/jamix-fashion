import { useEffect, useRef, useState } from 'react';

import { MAX_QTY, defaultPerson, peopleFor, sizesFor, whatsAppUrl } from '../lib/bag.js';
import { useBag } from '../hooks/useBag.jsx';

/**
 * Product quick view: photo gallery + "para quién / talla / cantidad" + add to the family look.
 * Built on the native <dialog>: Esc closes it and focus stays inside while it's open.
 */
export default function QuickView({ product, apiBaseUrl, whatsappNumber, onClose }) {
  const dialogRef = useRef(null);
  const bag = useBag();
  const [photo, setPhoto] = useState(0);
  const [person, setPerson] = useState(() => defaultPerson(product, bag.lastPerson));
  const people = peopleFor(product);
  const [size, setSize] = useState('');
  const [qty, setQty] = useState(1);

  const images = [product.imageUrl, ...(product.gallery ?? [])].filter(Boolean);
  const sizes = sizesFor(product.category);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  function onKeyDown(event) {
    if (images.length < 2) return;
    // Arrows already mean something in the size select and the person radios.
    if (['INPUT', 'SELECT', 'TEXTAREA'].includes(event.target.tagName)) return;
    if (event.key === 'ArrowRight') setPhoto((p) => (p + 1) % images.length);
    if (event.key === 'ArrowLeft') setPhoto((p) => (p - 1 + images.length) % images.length);
  }

  function submit(event) {
    event.preventDefault();
    bag.add(product, { person, size, qty });
    dialogRef.current?.close();
  }

  const titleId = `qv-title-${product.slug}`;

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onClose={onClose}
      onKeyDown={onKeyDown}
      // Click on the backdrop (the dialog element itself, outside the panel) closes it.
      onClick={(e) => e.target === dialogRef.current && dialogRef.current.close()}
      className="m-auto w-[min(56rem,calc(100%-2rem))] max-h-[calc(100dvh-2rem)] overflow-hidden rounded-3xl bg-jx-white p-0 text-jx-charcoal shadow-2xl backdrop:bg-jx-charcoal/60 backdrop:backdrop-blur-sm open:motion-safe:animate-fade-in"
    >
      <div className="grid max-h-[calc(100dvh-2rem)] overflow-y-auto md:grid-cols-2">
        {/* Gallery */}
        <div className="relative bg-jx-natural">
          <div className="aspect-[3/4]">
            {images.length > 0 ? (
              <img
                key={images[photo]}
                src={`${apiBaseUrl}${images[photo]}`}
                alt={`${product.name}, foto ${photo + 1} de ${images.length}`}
                className="h-full w-full object-cover motion-safe:animate-fade-in"
              />
            ) : (
              <div className="flex h-full items-center justify-center font-display text-lg">Foto próximamente</div>
            )}
          </div>
          {images.length > 1 && (
            <div className="absolute inset-x-0 bottom-0 flex justify-center gap-2 bg-gradient-to-t from-jx-charcoal/50 to-transparent p-3">
              {images.map((src, i) => (
                <button
                  key={src}
                  type="button"
                  onClick={() => setPhoto(i)}
                  aria-label={`Ver foto ${i + 1} de ${images.length}`}
                  aria-current={i === photo}
                  className={`h-14 w-11 overflow-hidden rounded-lg border-2 transition ${
                    i === photo ? 'border-jx-white' : 'border-transparent opacity-70 hover:opacity-100'
                  }`}
                >
                  <img src={`${apiBaseUrl}${src}`} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Details + form */}
        <form onSubmit={submit} className="flex flex-col gap-5 p-6 sm:p-8">
          <div className="flex items-start justify-between gap-4">
            <div>
              {product.type && (
                <p className="text-[0.7rem] font-medium uppercase tracking-wider text-jx-gold-dark">{product.type}</p>
              )}
              <h2 id={titleId} className="font-display text-2xl font-semibold leading-tight">{product.name}</h2>
            </div>
            <button
              type="button"
              onClick={() => dialogRef.current?.close()}
              aria-label="Cerrar"
              className="-mr-2 -mt-2 rounded-full p-2 text-2xl leading-none text-jx-muted hover:bg-jx-crudo hover:text-jx-charcoal"
            >
              ×
            </button>
          </div>

          <div>
            <p className="text-xl font-semibold">{product.priceLabel}</p>
            {product.wholesaleLabel && (
              <p className="text-sm text-jx-muted">
                Mayorista {product.wholesaleLabel} desde {product.wholesaleMinUnits} unid. (sumando toda la familia)
              </p>
            )}
            {product.description && <p className="mt-3 text-sm leading-relaxed text-jx-charcoal-soft">{product.description}</p>}
          </div>

          {product.inStock ? (
            <>
              <fieldset className="flex flex-col gap-2">
                <legend className="mb-2 text-sm font-medium">¿Para quién es?</legend>
                <div className="flex flex-wrap gap-2">
                  {people.map((p) => (
                    <label key={p} className="cursor-pointer">
                      <input type="radio" name="person" value={p} checked={person === p}
                        onChange={() => setPerson(p)} className="peer sr-only" />
                      <span className="inline-block rounded-full border border-jx-natural px-3 py-1.5 text-sm transition peer-checked:border-jx-charcoal peer-checked:bg-jx-charcoal peer-checked:text-jx-white peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-jx-charcoal">
                        {p}
                      </span>
                    </label>
                  ))}
                </div>
              </fieldset>

              <div className="grid grid-cols-[1fr_auto] gap-4">
                <label className="flex flex-col gap-1.5 text-sm font-medium">
                  Talla
                  <select
                    required
                    value={size}
                    onChange={(e) => setSize(e.target.value)}
                    className="rounded-xl border border-jx-natural bg-jx-white px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige"
                  >
                    <option value="">Elige una talla</option>
                    {sizes.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </label>
                <div className="flex flex-col gap-1.5 text-sm font-medium">
                  <span id={`${titleId}-qty`}>Cantidad</span>
                  <div role="group" aria-labelledby={`${titleId}-qty`} className="flex items-center rounded-xl border border-jx-natural">
                    <button type="button" onClick={() => setQty((q) => Math.max(1, q - 1))} aria-label="Quitar una"
                      className="px-3 py-3 text-lg leading-none hover:bg-jx-crudo">−</button>
                    <output className="w-8 text-center tabular-nums" aria-live="polite">{qty}</output>
                    <button type="button" onClick={() => setQty((q) => Math.min(MAX_QTY, q + 1))} aria-label="Agregar una"
                      className="px-3 py-3 text-lg leading-none hover:bg-jx-crudo">+</button>
                  </div>
                </div>
              </div>

              <button
                type="submit"
                className="mt-auto rounded-full bg-jx-charcoal px-6 py-3.5 text-sm font-semibold text-jx-white transition hover:bg-jx-charcoal-soft"
              >
                Agregar al look de {person}
              </button>
            </>
          ) : (
            <p className="rounded-xl bg-jx-crudo px-4 py-3 text-sm">Esta prenda está agotada por ahora.</p>
          )}

          <a
            href={whatsAppUrl(
              whatsappNumber,
              product.inStock
                ? `Hola Jamix Fashion, tengo una consulta sobre: ${product.name} (${product.priceLabel})`
                : `Hola Jamix Fashion, ¿cuándo vuelve ${product.name}?`,
            )}
            target="_blank"
            rel="noopener noreferrer"
            className="text-center text-sm font-medium text-jx-gold-dark underline underline-offset-4 hover:text-jx-charcoal"
          >
            {product.inStock ? 'Consultar esta prenda por WhatsApp' : 'Avísame cuando vuelva'}
          </a>
        </form>
      </div>
    </dialog>
  );
}
