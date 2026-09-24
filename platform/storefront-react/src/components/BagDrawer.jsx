import { useEffect, useRef } from 'react';

import { UNSURE_SIZE, formatSoles, whatsAppMessage, whatsAppUrl } from '../lib/bag.js';
import { useBag } from '../hooks/useBag.jsx';

/** Side panel with the family look, grouped by person, and the "send it all by WhatsApp" button. */
export default function BagDrawer({ apiBaseUrl, whatsappNumber }) {
  const bag = useBag();
  const dialogRef = useRef(null);
  const { summary } = bag;

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (bag.open && !dialog.open) dialog.showModal();
    if (!bag.open && dialog.open) dialog.close();
  }, [bag.open]);

  function clearBag() {
    if (window.confirm('¿Vaciar todo el look?')) bag.clear();
  }

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="bag-title"
      onClose={() => bag.setOpen(false)}
      onClick={(e) => e.target === dialogRef.current && dialogRef.current.close()}
      className="fixed inset-y-0 right-0 left-auto m-0 h-full max-h-none w-full max-w-md bg-jx-white p-0 text-jx-charcoal shadow-2xl backdrop:bg-jx-charcoal/50 open:motion-safe:animate-fade-in"
    >
      <div className="flex h-full flex-col">
        <header className="flex items-center justify-between border-b border-jx-natural px-6 py-5">
          <div>
            <h2 id="bag-title" className="font-display text-2xl font-semibold">El look de tu familia</h2>
            <p className="text-sm text-jx-muted">{summary.units} {summary.units === 1 ? 'prenda' : 'prendas'}</p>
          </div>
          <button type="button" onClick={() => bag.setOpen(false)} aria-label="Cerrar"
            className="rounded-full p-2 text-2xl leading-none text-jx-muted hover:bg-jx-crudo hover:text-jx-charcoal">×</button>
        </header>

        {summary.units === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 px-8 text-center">
            <p className="font-display text-xl">Tu look está vacío</p>
            <p className="text-sm text-jx-muted">
              Abre una prenda, elige para quién es y su talla. Al final lo envías todo en un solo WhatsApp.
            </p>
            <button type="button" onClick={() => bag.setOpen(false)}
              className="mt-2 rounded-full border border-jx-charcoal px-5 py-2 text-sm font-medium hover:bg-jx-crudo">
              Ver el catálogo
            </button>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-y-auto px-6 py-4">
              {[...summary.byPerson].map(([person, lines]) => (
                <section key={person} className="mb-6" aria-label={`Look de ${person}`}>
                  <h3 className="mb-3 text-xs font-semibold uppercase tracking-[0.2em] text-jx-gold-dark">{person}</h3>
                  <ul className="flex flex-col gap-3">
                    {lines.map((l) => (
                      <li key={l.key} className="flex gap-3">
                        <div className="h-20 w-16 shrink-0 overflow-hidden rounded-lg bg-jx-natural">
                          {l.imageUrl && <img src={`${apiBaseUrl}${l.imageUrl}`} alt="" className="h-full w-full object-cover" />}
                        </div>
                        <div className="flex flex-1 flex-col gap-1">
                          <div className="flex justify-between gap-2">
                            <p className="font-medium leading-tight">{l.name}</p>
                            <p className="shrink-0 font-semibold tabular-nums">
                              {l.priceIsRange && <span className="text-xs font-normal text-jx-muted">desde </span>}
                              {formatSoles(l.subtotal)}
                            </p>
                          </div>
                          <p className="text-xs text-jx-muted">
                            {l.size === UNSURE_SIZE ? 'Talla: asesórenme' : `Talla ${l.size}`}
                            {l.wholesale && <span className="ml-2 rounded-full bg-jx-crudo px-2 py-0.5 text-jx-gold-dark">mayorista</span>}
                          </p>
                          <div className="mt-1 flex items-center gap-3">
                            <div className="flex items-center rounded-lg border border-jx-natural text-sm">
                              <button type="button" onClick={() => bag.setQty(l.key, l.qty - 1)}
                                aria-label={`Quitar una unidad de ${l.name}`} className="px-2.5 py-1 hover:bg-jx-crudo">−</button>
                              <span className="w-7 text-center tabular-nums">{l.qty}</span>
                              <button type="button" onClick={() => bag.setQty(l.key, l.qty + 1)}
                                aria-label={`Agregar una unidad de ${l.name}`} className="px-2.5 py-1 hover:bg-jx-crudo">+</button>
                            </div>
                            <button type="button" onClick={() => bag.remove(l.key)}
                              className="text-xs text-jx-muted underline underline-offset-2 hover:text-jx-charcoal">Quitar</button>
                          </div>
                        </div>
                      </li>
                    ))}
                  </ul>
                </section>
              ))}
            </div>

            <footer className="border-t border-jx-natural bg-jx-crudo/60 px-6 py-5">
              {summary.nudges.map((n) => (
                <p key={n.productId} className="mb-2 rounded-lg bg-jx-white px-3 py-2 text-xs">
                  Agrega {n.missing} {n.missing === 1 ? 'unidad más' : 'unidades más'} de <strong>{n.name}</strong> y
                  todas pasan a precio mayorista.
                </p>
              ))}
              {summary.wholesaleNames.length > 0 && (
                <p className="mb-2 text-xs text-jx-gold-dark">
                  Precio mayorista aplicado: {summary.wholesaleNames.join(', ')}.
                </p>
              )}
              <div className="flex items-baseline justify-between">
                <span className="text-sm">Total{summary.estimated ? ' estimado' : ''}</span>
                <span className="font-display text-3xl font-semibold tabular-nums">{formatSoles(summary.total)}</span>
              </div>
              {summary.estimated && <p className="text-xs text-jx-muted">Algunos precios varían según la talla.</p>}
              <a
                href={whatsAppUrl(whatsappNumber, whatsAppMessage(summary))}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 flex items-center justify-center gap-2 rounded-full bg-jx-whatsapp px-6 py-3.5 text-sm font-semibold text-jx-white transition hover:brightness-110"
              >
                Enviar mi look por WhatsApp
              </a>
              <button type="button" onClick={clearBag}
                className="mt-3 w-full text-center text-xs text-jx-muted underline underline-offset-2 hover:text-jx-charcoal">
                Vaciar el look
              </button>
            </footer>
          </>
        )}
      </div>
    </dialog>
  );
}
