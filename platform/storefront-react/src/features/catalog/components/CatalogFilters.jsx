import { m } from 'motion/react';
import { memo } from 'react';

import { CATEGORIES } from '../../../shared/config.js';
import { SORTS } from '../lib/filters.js';

/**
 * Filter pill whose dark background is ONE shared element per group (layoutId): selecting another
 * pill makes it glide there instead of one pill switching off and another switching on.
 */
function FilterPill({ group, selected, onClick, children }) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={`relative isolate shrink-0 rounded-full border px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-jx-charcoal ${
        selected ? 'border-jx-charcoal text-jx-white' : 'border-jx-natural bg-jx-white text-jx-charcoal hover:border-jx-charcoal'
      }`}
    >
      {selected && (
        <m.span
          layoutId={`filter-pill-${group}`}
          aria-hidden="true"
          className="absolute inset-0 -z-10 rounded-full bg-jx-charcoal"
          transition={{ type: 'spring', stiffness: 420, damping: 34 }}
        />
      )}
      {children}
    </button>
  );
}

/** Presentational: renders the filter controls; all state lives in useCatalogFilters. */
function CatalogFilters({ filters, counts, types, onUpdate, onSelectCategory }) {
  return (
    <div className="mb-8 flex flex-col gap-4 rounded-2xl bg-jx-white p-4 sm:p-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div role="group" aria-label="Filtrar por categoría" className="flex gap-2 overflow-x-auto pb-1">
          {CATEGORIES.map(({ slug, label }) => (
            <FilterPill key={slug} group="category" selected={filters.category === slug} onClick={() => onSelectCategory(slug)}>
              {label}
              {counts[slug] != null && <span className="ml-1.5 opacity-60">{counts[slug]}</span>}
            </FilterPill>
          ))}
        </div>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <label className="relative flex-1 sm:w-64 sm:flex-none">
            <span className="sr-only">Buscar prendas</span>
            <input
              type="search"
              value={filters.query}
              onChange={(e) => onUpdate({ query: e.target.value })}
              placeholder="Buscar: vestido, top, niña…"
              className="w-full rounded-full border border-jx-natural px-4 py-2 text-sm outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige"
            />
          </label>
          <label className="flex items-center gap-2 text-sm">
            <span className="sr-only sm:not-sr-only">Ordenar</span>
            <select
              value={filters.sort}
              onChange={(e) => onUpdate({ sort: e.target.value })}
              className="flex-1 rounded-full border border-jx-natural bg-jx-white px-4 py-2 outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige"
            >
              {SORTS.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div role="group" aria-label="Filtrar por tipo de prenda" className="flex gap-2 overflow-x-auto pb-1">
          <FilterPill group="type" selected={filters.type === 'all'} onClick={() => onUpdate({ type: 'all' })}>
            Todos los tipos
          </FilterPill>
          {types.map((t) => (
            <FilterPill key={t} group="type" selected={filters.type === t} onClick={() => onUpdate({ type: t })}>
              {t}
            </FilterPill>
          ))}
        </div>
        <label className="flex shrink-0 cursor-pointer items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={filters.onlyAvailable}
            onChange={(e) => onUpdate({ onlyAvailable: e.target.checked })}
            className="h-4 w-4 accent-jx-charcoal"
          />
          Solo disponibles
        </label>
      </div>
    </div>
  );
}

export default memo(CatalogFilters);
