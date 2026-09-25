// Pure logic for the "look de tu familia" bag: no React, no browser APIs, so it's unit-tested
// with `node --test` (see bag.test.js).

export const PEOPLE = ['Mamá', 'Papá', 'Hija', 'Hijo', 'Bebé', 'Abuela', 'Abuelo', 'Otra persona'];

// "¿Para quién es?" only offers family members the garment fits: no dress in Papá's look.
// Adult women's clothes include Hija (teenagers wear S/M); kids' clothes include Bebé.
const PEOPLE_BY_FIT = {
  'femenino/adult': ['Mamá', 'Abuela', 'Hija', 'Otra persona'],
  'femenino/kids': ['Hija', 'Bebé', 'Otra persona'],
  'masculino/adult': ['Papá', 'Abuelo', 'Hijo', 'Otra persona'],
  'masculino/kids': ['Hijo', 'Bebé', 'Otra persona'],
};

export function peopleFor(product) {
  const age = product.category === 'ninos' ? 'kids' : 'adult';
  return PEOPLE_BY_FIT[`${product.gender}/${age}`] ?? PEOPLE; // unisex or unknown: everyone
}

/** The person to preselect: the last one used if this garment fits them, otherwise the first option. */
export function defaultPerson(product, lastPerson) {
  const people = peopleFor(product);
  return people.includes(lastPerson) ? lastPerson : people[0];
}
export const UNSURE_SIZE = 'Asesórenme';
const ADULT_SIZES = ['S', 'M', 'L', 'XL'];
const KIDS_SIZES = ['2', '4', '6', '8', '10', '12', '14', '16'];
export const MAX_QTY = 99;

export function sizesFor(category) {
  return [...(category === 'ninos' ? KIDS_SIZES : ADULT_SIZES), UNSURE_SIZE];
}

const soles = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 });
export const formatSoles = (amount) => `S/ ${soles.format(amount)}`;

export const itemKey = (productId, person, size) => `${productId}|${person}|${size}`;

const clampQty = (qty) => Math.max(0, Math.min(MAX_QTY, Math.trunc(Number(qty) || 0)));

/** Snapshot of the product fields the bag needs (kept in localStorage). */
function snapshot(product) {
  return {
    productId: product.id,
    slug: product.slug,
    name: product.name,
    category: product.category,
    imageUrl: product.imageUrl ?? null,
    price: Number(product.price),
    priceMax: product.priceMax == null ? null : Number(product.priceMax),
  };
}

/** Adds (or merges into an existing line for the same product, person and size). */
export function addItem(items, product, { person, size, qty = 1 }) {
  const key = itemKey(product.id, person, size);
  const existing = items.find((i) => i.key === key);
  if (existing) {
    return items.map((i) => (i.key === key ? { ...i, qty: clampQty(i.qty + qty) || 1 } : i));
  }
  return [...items, { key, person, size, qty: clampQty(qty) || 1, ...snapshot(product) }];
}

export function setQty(items, key, qty) {
  const next = clampQty(qty);
  return next === 0 ? removeItem(items, key) : items.map((i) => (i.key === key ? { ...i, qty: next } : i));
}

export function removeItem(items, key) {
  return items.filter((i) => i.key !== key);
}

/**
 * Refreshes prices from the live catalogue and drops lines whose product no longer exists,
 * so a bag saved days ago never shows stale prices.
 */
export function syncWithCatalog(items, products) {
  const byId = new Map(products.map((p) => [p.id, p]));
  return items
    .filter((i) => byId.has(i.productId))
    .map((i) => ({ ...i, ...snapshot(byId.get(i.productId)), key: i.key, person: i.person, size: i.size, qty: i.qty }));
}

/** Totals, and the lines grouped by person (every unit at the retail price). */
export function summarize(items) {
  let estimated = false;
  const lines = items.map((i) => {
    // Size-dependent price ranges: we charge the "from" price and say it's an estimate.
    const priceIsRange = i.priceMax != null && i.priceMax !== i.price;
    if (priceIsRange) estimated = true;
    return { ...i, unitPrice: i.price, priceIsRange, subtotal: i.price * i.qty };
  });

  const byPerson = new Map();
  for (const line of lines) {
    if (!byPerson.has(line.person)) byPerson.set(line.person, []);
    byPerson.get(line.person).push(line);
  }

  return {
    lines,
    byPerson,
    units: lines.reduce((sum, l) => sum + l.qty, 0),
    total: lines.reduce((sum, l) => sum + l.subtotal, 0),
    estimated,
  };
}

/** The whole family's order as one WhatsApp message. */
export function whatsAppMessage(summary) {
  const out = ['Hola Jamix Fashion, quiero armar este look para mi evento:', ''];
  for (const [person, lines] of summary.byPerson) {
    out.push(`*${person}*`);
    for (const l of lines) {
      const size = l.size === UNSURE_SIZE ? 'talla: asesórenme' : `talla ${l.size}`;
      const from = l.priceIsRange ? 'desde ' : '';
      out.push(`- ${l.name} (${size}) x${l.qty}: ${from}${formatSoles(l.subtotal)}`);
    }
    out.push('');
  }
  out.push(`Total${summary.estimated ? ' estimado' : ''}: ${formatSoles(summary.total)} (${summary.units} prendas)`);
  if (summary.estimated) out.push('Algunos precios varían según la talla.');
  out.push('¿Me confirman disponibilidad?');
  return out.join('\n');
}

export const whatsAppUrl = (number, text) => `https://wa.me/${number}?text=${encodeURIComponent(text)}`;
