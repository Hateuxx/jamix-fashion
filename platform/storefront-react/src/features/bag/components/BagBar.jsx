import { useBagActions, useBagContent, useBagUi } from '../BagProvider.jsx';
import { formatSoles } from '../lib/bag.js';

/** Header button with the item count (all screens). */
export function BagButton() {
  const { summary } = useBagContent();
  const { openBag } = useBagActions();
  return (
    <button
      type="button"
      onClick={openBag}
      className="relative flex items-center gap-2 rounded-full border border-jx-charcoal px-4 py-2 text-sm font-medium transition hover:bg-jx-crudo"
    >
      Mi look
      <span
        key={summary.units} // re-mounts so the count "pops" whenever it changes
        className="inline-flex min-w-6 items-center justify-center rounded-full bg-jx-charcoal px-1.5 text-xs text-jx-white tabular-nums motion-safe:animate-fade-in"
      >
        {summary.units}
      </span>
    </button>
  );
}

/** Sticky bottom bar on phones, visible as soon as the look has something in it. */
export function BagBar() {
  const { summary } = useBagContent();
  const { openBag } = useBagActions();
  if (summary.units === 0) return null;
  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-jx-natural bg-jx-white/95 p-3 backdrop-blur md:hidden motion-safe:animate-fade-in">
      <button
        type="button"
        onClick={openBag}
        className="flex w-full items-center justify-between rounded-full bg-jx-charcoal px-5 py-3 text-sm font-semibold text-jx-white"
      >
        <span>Tu look · {summary.units} {summary.units === 1 ? 'prenda' : 'prendas'}</span>
        <span className="tabular-nums">{formatSoles(summary.total)} →</span>
      </button>
    </div>
  );
}

/** Short confirmation after adding (announced to screen readers). */
export function Toast() {
  const { toast } = useBagUi();
  const { openBag } = useBagActions();
  return (
    <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-20 z-50 flex justify-center px-4 md:bottom-6">
      {toast && (
        <div key={toast.id}
          className="pointer-events-auto flex items-center gap-4 rounded-full bg-jx-charcoal px-5 py-3 text-sm text-jx-white shadow-lg motion-safe:animate-fade-in">
          <span>{toast.message}</span>
          <button type="button" onClick={openBag} className="font-semibold text-jx-beige underline underline-offset-4">
            Ver look
          </button>
        </div>
      )}
    </div>
  );
}
