import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

import { addItem, removeItem, setQty, summarize, syncWithCatalog } from '../lib/bag.js';

const STORAGE_KEY = 'jamix.bag.v1';
const BagContext = createContext(null);

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
  const toastTimer = useRef();

  useEffect(() => save(items), [items]);

  const notify = useCallback((message) => {
    clearTimeout(toastTimer.current);
    setToast({ id: Date.now(), message });
    toastTimer.current = setTimeout(() => setToast(null), 3500);
  }, []);

  // Stable actions (functional updates, no dependency on `items`): components can safely list
  // them as effect dependencies without re-running the effect on every bag change.
  const add = useCallback(
    (product, choice) => {
      setItems((current) => addItem(current, product, choice));
      setLastPerson(choice.person);
      notify(`${product.name} agregado al look de ${choice.person}`);
    },
    [notify],
  );
  const changeQty = useCallback((key, qty) => setItems((current) => setQty(current, key, qty)), []);
  const remove = useCallback((key) => setItems((current) => removeItem(current, key)), []);
  const clear = useCallback(() => setItems([]), []);
  const sync = useCallback((products) => setItems((current) => syncWithCatalog(current, products)), []);

  const summary = useMemo(() => summarize(items), [items]);

  const value = useMemo(
    () => ({ items, summary, open, setOpen, toast, lastPerson, add, setQty: changeQty, remove, clear, sync }),
    [items, summary, open, toast, lastPerson, add, changeQty, remove, clear, sync],
  );

  return <BagContext.Provider value={value}>{children}</BagContext.Provider>;
}

export function useBag() {
  const bag = useContext(BagContext);
  if (!bag) throw new Error('useBag must be used inside <BagProvider>');
  return bag;
}
