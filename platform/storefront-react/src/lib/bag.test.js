import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import {
  UNSURE_SIZE, addItem, defaultPerson, peopleFor, removeItem, setQty, sizesFor, summarize, syncWithCatalog,
  whatsAppMessage,
} from './bag.js';

const rita = { id: 2, slug: 'vestido-rita', name: 'Vestido Rita', category: 'ella', imageUrl: '/img/r.jpeg',
  price: 45, priceMax: null, wholesalePrice: 33, wholesalePriceMax: null, wholesaleMinUnits: 6 };
const tunica = { id: 4, slug: 'vestido-tunica', name: 'Vestido Túnica', category: 'ninos', imageUrl: '/img/t.jpeg',
  price: 50, priceMax: 60, wholesalePrice: 32, wholesalePriceMax: 36, wholesaleMinUnits: 6 };
const top = { id: 8, slug: 'top-naty', name: 'Top Naty', category: 'ella', imageUrl: null,
  price: 29, priceMax: null, wholesalePrice: null, wholesalePriceMax: null, wholesaleMinUnits: 6 };

describe('bag', () => {
  it('merges the same product, person and size into one line', () => {
    let items = addItem([], rita, { person: 'Mamá', size: 'M', qty: 1 });
    items = addItem(items, rita, { person: 'Mamá', size: 'M', qty: 2 });
    items = addItem(items, rita, { person: 'Hija', size: 'S', qty: 1 });
    assert.equal(items.length, 2);
    assert.equal(items[0].qty, 3);
  });

  it('applies wholesale when the same product reaches 6 units across the family', () => {
    let items = addItem([], rita, { person: 'Mamá', size: 'M', qty: 3 });
    items = addItem(items, rita, { person: 'Abuela', size: 'L', qty: 2 });
    let s = summarize(items);
    assert.equal(s.total, 5 * 45);
    assert.deepEqual(s.nudges, [{ productId: 2, name: 'Vestido Rita', missing: 1 }]);

    items = addItem(items, rita, { person: 'Hija', size: 'S', qty: 1 });
    s = summarize(items);
    assert.equal(s.total, 6 * 33);
    assert.deepEqual(s.wholesaleNames, ['Vestido Rita']);
    assert.equal(s.nudges.length, 0);
  });

  it('marks size-dependent prices as an estimate and ignores products without wholesale', () => {
    let items = addItem([], tunica, { person: 'Hija', size: '8', qty: 1 });
    items = addItem(items, top, { person: 'Mamá', size: 'M', qty: 6 });
    const s = summarize(items);
    assert.equal(s.estimated, true);
    assert.equal(s.total, 50 + 6 * 29);
    assert.equal(s.lines.find((l) => l.productId === 8).wholesale, false);
  });

  it('setQty to 0 removes the line; quantities are clamped', () => {
    let items = addItem([], rita, { person: 'Mamá', size: 'M', qty: 500 });
    assert.equal(items[0].qty, 99);
    items = setQty(items, items[0].key, 0);
    assert.equal(items.length, 0);
    assert.deepEqual(removeItem([], 'x'), []);
  });

  it('syncs prices with the live catalogue and drops removed products', () => {
    let items = addItem([], rita, { person: 'Mamá', size: 'M', qty: 1 });
    items = addItem(items, top, { person: 'Mamá', size: 'M', qty: 1 });
    const synced = syncWithCatalog(items, [{ ...rita, price: 50 }]);
    assert.equal(synced.length, 1);
    assert.equal(synced[0].price, 50);
    assert.equal(synced[0].person, 'Mamá');
  });

  it('builds one WhatsApp message grouped by person', () => {
    let items = addItem([], rita, { person: 'Mamá', size: 'M', qty: 1 });
    items = addItem(items, tunica, { person: 'Hija', size: UNSURE_SIZE, qty: 1 });
    const text = whatsAppMessage(summarize(items));
    assert.match(text, /\*Mamá\*\n- Vestido Rita \(talla M\) x1: S\/ 45/);
    assert.match(text, /\*Hija\*\n- Vestido Túnica \(talla: asesórenme\) x1: desde S\/ 50/);
    assert.match(text, /Total estimado: S\/ 95 \(2 prendas\)/);
  });

  it('only offers family members the garment fits', () => {
    const dress = { category: 'ella', gender: 'femenino' };
    const menSet = { category: 'el', gender: 'masculino' };
    const girlsDress = { category: 'ninos', gender: 'femenino' };
    const boysSet = { category: 'ninos', gender: 'masculino' };
    assert.deepEqual(peopleFor(dress), ['Mamá', 'Abuela', 'Hija', 'Otra persona']);
    assert.ok(!peopleFor(dress).includes('Papá'));
    assert.deepEqual(peopleFor(menSet), ['Papá', 'Abuelo', 'Hijo', 'Otra persona']);
    assert.deepEqual(peopleFor(girlsDress), ['Hija', 'Bebé', 'Otra persona']);
    assert.deepEqual(peopleFor(boysSet), ['Hijo', 'Bebé', 'Otra persona']);
    assert.equal(peopleFor({ category: 'ella', gender: 'unisex' }).length, 8);
  });

  it('keeps the last person only when the garment fits them', () => {
    assert.equal(defaultPerson({ category: 'ella', gender: 'femenino' }, 'Hija'), 'Hija');
    assert.equal(defaultPerson({ category: 'el', gender: 'masculino' }, 'Mamá'), 'Papá');
  });

  it('offers kids sizes for the Niños category', () => {
    assert.deepEqual(sizesFor('ninos').slice(0, 2), ['2', '4']);
    assert.equal(sizesFor('ella').at(-1), UNSURE_SIZE);
  });
});
