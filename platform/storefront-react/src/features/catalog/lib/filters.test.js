import assert from 'node:assert/strict';
import { describe, it } from 'node:test';

import { DEFAULT_FILTERS, applyFilters, filtersFromSearch, filtersToSearch, typesIn } from './filters.js';

const products = [
  { id: 1, name: 'Falda Bobo', type: 'Faldas', category: 'ella', categoryLabel: 'Ella', price: 25, inStock: true },
  { id: 2, name: 'Vestido Rita', type: 'Vestidos largos', category: 'ella', categoryLabel: 'Ella', price: 45, inStock: true },
  { id: 3, name: 'Vestido Túnica', type: 'Vestidos de niña', category: 'ninos', categoryLabel: 'Niños', price: 50, inStock: true },
  { id: 4, name: 'Short Bolsillo', type: 'Shorts', category: 'el', categoryLabel: 'Él', price: 52, inStock: false },
];
const f = (over) => ({ ...DEFAULT_FILTERS, ...over });
const ids = (list) => list.map((p) => p.id);

describe('filters', () => {
  it('search ignores accents and case, and needs every word', () => {
    assert.deepEqual(ids(applyFilters(products, f({ query: 'TUNICA' }))), [3]);
    assert.deepEqual(ids(applyFilters(products, f({ query: 'vestido nina' }))), [3]);
    assert.deepEqual(ids(applyFilters(products, f({ query: 'vestido' }))), [2, 3]);
  });

  it('combines category, type and availability', () => {
    assert.deepEqual(ids(applyFilters(products, f({ category: 'ella', type: 'Faldas' }))), [1]);
    assert.deepEqual(ids(applyFilters(products, f({ onlyAvailable: true }))), [1, 2, 3]);
  });

  it('sorts by price and name without mutating the input', () => {
    assert.deepEqual(ids(applyFilters(products, f({ sort: 'price-desc' }))), [4, 3, 2, 1]);
    assert.deepEqual(ids(applyFilters(products, f({ sort: 'name' }))), [1, 4, 2, 3]);
    assert.deepEqual(ids(products), [1, 2, 3, 4]);
  });

  it('lists the garment types of a category', () => {
    assert.deepEqual(typesIn(products, 'ella'), ['Faldas', 'Vestidos largos']);
  });

  it('round-trips through the URL, keeping unrelated params and dropping defaults', () => {
    const search = filtersToSearch(f({ category: 'ninos', query: 'túnica', onlyAvailable: true }), '?utm=ig');
    assert.equal(search, '?utm=ig&categoria=ninos&q=t%C3%BAnica&disponibles=1');
    assert.deepEqual(filtersFromSearch(search, ['ella', 'el', 'ninos']), f({ category: 'ninos', query: 'túnica', onlyAvailable: true }));
    assert.equal(filtersToSearch(DEFAULT_FILTERS), '');
    assert.equal(filtersFromSearch('?categoria=mascotas', ['ella']).category, 'all');
  });
});
