import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import { addItem, removeItem, setQty, summarize, syncWithCatalog } from './lib/bag.js';

const STORAGE_KEY = 'jamix.bag.v1';
const TOAST_MS = 3500;

/*
 * Three contexts instead of one, split by how often each part changes, so a component only
 * re-renders for the data it reads:
 *   - actions: stable functions, never change   -> the catalogue can use them without re-rendering
 *   - content: items + totals, change on add/remove
 *   - ui:      drawer open/closed, toast, last person; change often (the toast every few seconds)
 */
const BagActionsContext = createContext(null);
const BagContentContext = createContext(null);
const BagUiContext = createContext(null);

// localStorage can be missing or throw (private mode, blocked storage): the bag then just
// lives for this visit instead of breaking the store.
function load() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    return Array.isArray(parsed) ? parsed.filter((i) => i && i.key && i.productId && i.qty > 0) : [];
  } catch {
    return [];
  }
}

function save(items) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // ignore: see load()
  }
}

export function BagProvider({ children }) {
  const [items, setItems] = useState(load);
  const [open, setOpen] = useState(false);
  const [toast, setToast] = useState(null);
  const [lastPerson, setLastPerson] = useState(() => items.at(-1)?.person ?? 'Mamá');
  const toastTimer = useRef(undefined);

  useEffect(() => save(items), [items]);
  useEffect(() => () => clearTimeout(toastTimer.current), []);

  // Only functional updates: no dependency on current state, so the object below is created once.
  const actions = useMemo(() => {
    const notify = (message) => {
      clearTimeout(toastTimer.current);
      setToast({ id: Date.now(), message });
      toastTimer.current = setTimeout(() => setToast(null), TOAST_MS);
    };
    return {
      add(product, choice) {
        setItems((current) => addItem(current, product, choice));
        setLastPerson(choice.person);
        notify(`${product.name} agregado al look de ${choice.person}`);
      },
      setQty: (key, qty) => setItems((current) => setQty(current, key, qty)),
      remove: (key) => setItems((current) => removeItem(current, key)),
      clear: () => setItems([]),
      /** Refreshes prices of a bag saved in an earlier visit and drops products that no longer exist. */
      sync: (products) => setItems((current) => syncWithCatalog(current, products)),
      openBag: () => setOpen(true),
      closeBag: () => setOpen(false),
    };
  }, []);

  const content = useMemo(() => ({ items, summary: summarize(items) }), [items]);
  const ui = useMemo(() => ({ open, toast, lastPerson }), [open, toast, lastPerson]);

  return (
    <BagActionsContext.Provider value={actions}>
      <BagContentContext.Provider value={content}>
        <BagUiContext.Provider value={ui}>{children}</BagUiContext.Provider>
      </BagContentContext.Provider>
    </BagActionsContext.Provider>
  );
}

function useRequired(context, name) {
  const value = useContext(context);
  if (!value) throw new Error(`${name} must be used inside <BagProvider>`);
  return value;
}

/** add, setQty, remove, clear, sync, openBag, closeBag. Stable: never causes a re-render. */
export const useBagActions = () => useRequired(BagActionsContext, 'useBagActions');

/** { items, summary }: re-renders when the bag's content changes. */
export const useBagContent = () => useRequired(BagContentContext, 'useBagContent');

/** { open, toast, lastPerson }: re-renders on drawer and toast changes. */
export const useBagUi = () => useRequired(BagUiContext, 'useBagUi');
