<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import { assetUrl } from '../api/client.js';
import { useAuth } from '../composables/useAuth.js';
import { CATEGORY_LABELS, LOW_STOCK, normalize } from '../lib/format.js';
import MovementDialog from './MovementDialog.vue';
import MovementList from './MovementList.vue';

const { api, can } = useAuth();

const products = ref([]);
const types = ref([]);
const status = ref('loading');
const message = ref({ text: '', tone: 'info' });
const busy = reactive({}); // product id -> quick action in flight
const dialog = ref(null); // { product, initialType }
const history = ref(null); // { product, movements, loading }
const historyDialog = ref(null);

const filters = reactive({ query: '', category: 'all', type: 'all', level: 'all', sort: 'name' });

const totalUnits = computed(() => products.value.reduce((sum, p) => sum + p.stock, 0));
const lowCount = computed(() => products.value.filter((p) => p.stock > 0 && p.stock < LOW_STOCK).length);
const outCount = computed(() => products.value.filter((p) => p.stock === 0).length);
const typeOptions = computed(() => [
  ...new Set(products.value.filter((p) => filters.category === 'all' || p.category === filters.category).map((p) => p.type).filter(Boolean)),
]);

const visible = computed(() => {
  const words = normalize(filters.query).split(/\s+/).filter(Boolean);
  const list = products.value.filter((p) => {
    if (filters.category !== 'all' && p.category !== filters.category) return false;
    if (filters.type !== 'all' && p.type !== filters.type) return false;
    if (filters.level === 'low' && !(p.stock > 0 && p.stock < LOW_STOCK)) return false;
    if (filters.level === 'out' && p.stock !== 0) return false;
    const text = normalize(`${p.name} ${p.type ?? ''} ${p.slug}`);
    return words.every((w) => text.includes(w));
  });
  const sorters = {
    name: (a, b) => a.name.localeCompare(b.name, 'es'),
    'stock-asc': (a, b) => a.stock - b.stock || a.name.localeCompare(b.name, 'es'),
    'stock-desc': (a, b) => b.stock - a.stock || a.name.localeCompare(b.name, 'es'),
  };
  return [...list].sort(sorters[filters.sort]);
});

const filtered = computed(() => filters.query || filters.category !== 'all' || filters.type !== 'all' || filters.level !== 'all');

function resetFilters() {
  Object.assign(filters, { query: '', category: 'all', type: 'all', level: 'all' });
}

function show(text, tone = 'info') {
  message.value = { text, tone };
}

async function load() {
  status.value = 'loading';
  try {
    [products.value, types.value] = await Promise.all([api('/api/v1/admin/products'), api('/api/v1/admin/movement-types')]);
    status.value = 'ready';
  } catch (e) {
    status.value = 'error';
    show(e.message, 'error');
  }
}

function replace(updated) {
  const i = products.value.findIndex((p) => p.id === updated.id);
  if (i >= 0) products.value[i] = updated;
}

const allowed = (type) => types.value.some((t) => t.type === type && t.allowed);

/** One-tap +1 / −1 (a sale in the shop or a unit received). */
async function quick(product, type) {
  busy[product.id] = true;
  try {
    const result = await api(`/api/v1/admin/products/${product.id}/movements`, {
      method: 'POST',
      body: { type, quantity: 1 },
    });
    replace(result.product);
    show(`${product.name}: ${result.movement.typeLabel.toLowerCase()} registrada. Quedan ${result.product.stock}.`, 'ok');
  } catch (e) {
    show(e.message, 'error');
    if (e.status === 409) await load();
  } finally {
    busy[product.id] = false;
  }
}

function onSaved({ product, label }) {
  replace(product);
  show(`${product.name}: ${label.toLowerCase()} registrado. Stock actual: ${product.stock}.`, 'ok');
}

async function openHistory(product) {
  history.value = { product, movements: [], loading: true };
  await nextTick(); // the <dialog> only exists after Vue renders it
  historyDialog.value?.showModal();
  try {
    history.value.movements = await api(`/api/v1/admin/products/${product.id}/movements?limit=50`);
  } catch (e) {
    show(e.message, 'error');
  } finally {
    history.value.loading = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-6xl px-4 py-8 sm:px-6">
    <!-- Summary: each card is also a shortcut filter -->
    <div class="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
      <button type="button" class="rounded-2xl bg-jx-white p-4 text-left transition hover:ring-2 hover:ring-jx-beige" @click="resetFilters()">
        <p class="text-xs uppercase tracking-widest text-jx-muted">Prendas</p>
        <p class="mt-1 font-display text-3xl font-semibold">{{ products.length }}</p>
      </button>
      <div class="rounded-2xl bg-jx-white p-4">
        <p class="text-xs uppercase tracking-widest text-jx-muted">Unidades</p>
        <p class="mt-1 font-display text-3xl font-semibold">{{ totalUnits }}</p>
      </div>
      <button type="button" class="rounded-2xl bg-jx-white p-4 text-left transition hover:ring-2 hover:ring-jx-beige"
        :class="{ 'ring-2 ring-amber-400': filters.level === 'low' }" @click="filters.level = filters.level === 'low' ? 'all' : 'low'">
        <p class="text-xs uppercase tracking-widest text-jx-muted">Stock bajo (&lt; {{ LOW_STOCK }})</p>
        <p class="mt-1 font-display text-3xl font-semibold" :class="{ 'text-amber-700': lowCount }">{{ lowCount }}</p>
      </button>
      <button type="button" class="rounded-2xl bg-jx-white p-4 text-left transition hover:ring-2 hover:ring-jx-beige"
        :class="{ 'ring-2 ring-red-400': filters.level === 'out' }" @click="filters.level = filters.level === 'out' ? 'all' : 'out'">
        <p class="text-xs uppercase tracking-widest text-jx-muted">Agotadas</p>
        <p class="mt-1 font-display text-3xl font-semibold" :class="{ 'text-red-700': outCount }">{{ outCount }}</p>
      </button>
    </div>

    <!-- Filters -->
    <div class="mb-4 grid gap-3 rounded-2xl bg-jx-white p-4 sm:grid-cols-2 lg:grid-cols-5">
      <label class="lg:col-span-2">
        <span class="sr-only">Buscar</span>
        <input v-model="filters.query" type="search" placeholder="Buscar prenda: vestido, top, tunica…"
          class="w-full rounded-full border border-jx-natural px-4 py-2 text-sm outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
      </label>
      <select v-model="filters.category" aria-label="Categoría" @change="filters.type = 'all'"
        class="rounded-full border border-jx-natural bg-jx-white px-4 py-2 text-sm outline-none focus:border-jx-charcoal">
        <option value="all">Todas las categorías</option>
        <option v-for="(label, key) in CATEGORY_LABELS" :key="key" :value="key">{{ label }}</option>
      </select>
      <select v-model="filters.type" aria-label="Tipo de prenda"
        class="rounded-full border border-jx-natural bg-jx-white px-4 py-2 text-sm outline-none focus:border-jx-charcoal">
        <option value="all">Todos los tipos</option>
        <option v-for="t in typeOptions" :key="t" :value="t">{{ t }}</option>
      </select>
      <select v-model="filters.sort" aria-label="Ordenar"
        class="rounded-full border border-jx-natural bg-jx-white px-4 py-2 text-sm outline-none focus:border-jx-charcoal">
        <option value="name">Nombre (A–Z)</option>
        <option value="stock-asc">Menos stock primero</option>
        <option value="stock-desc">Más stock primero</option>
      </select>
    </div>

    <div class="mb-3 flex flex-wrap items-center justify-between gap-2 text-sm">
      <p class="text-jx-muted">{{ visible.length }} {{ visible.length === 1 ? 'prenda' : 'prendas' }}</p>
      <button v-if="filtered" type="button" class="text-jx-muted underline underline-offset-4 hover:text-jx-charcoal" @click="resetFilters()">
        Limpiar filtros
      </button>
    </div>

    <p v-if="message.text" role="status" class="mb-4 rounded-xl px-4 py-3 text-sm"
      :class="message.tone === 'error' ? 'bg-red-50 text-red-800' : message.tone === 'ok' ? 'bg-green-50 text-green-900' : 'bg-jx-white'">
      {{ message.text }}
    </p>

    <p v-if="status === 'loading'" class="text-jx-muted">Cargando inventario…</p>
    <div v-else-if="status === 'error'" class="flex items-center gap-4">
      <p>No se pudo cargar el inventario.</p>
      <button type="button" class="rounded-full border border-jx-charcoal px-4 py-2 text-sm" @click="load">Reintentar</button>
    </div>

    <ul v-else class="flex flex-col gap-2">
      <li v-for="p in visible" :key="p.id"
        class="flex flex-wrap items-center gap-3 rounded-2xl bg-jx-white p-3 sm:flex-nowrap sm:gap-4 sm:p-4">
        <img v-if="p.imageUrl" :src="assetUrl(p.imageUrl)" alt="" loading="lazy" class="h-16 w-12 shrink-0 rounded-lg object-cover" />
        <div v-else class="h-16 w-12 shrink-0 rounded-lg bg-jx-natural" />
        <div class="min-w-0 flex-1">
          <p class="truncate font-medium">{{ p.name }}</p>
          <p class="text-xs text-jx-muted">{{ CATEGORY_LABELS[p.category] }} · {{ p.type }} · S/ {{ p.price }}</p>
        </div>

        <div class="w-20 text-center">
          <p class="font-display text-3xl font-semibold tabular-nums"
            :class="p.stock === 0 ? 'text-red-700' : p.stock < LOW_STOCK ? 'text-amber-700' : ''">{{ p.stock }}</p>
          <p class="text-[0.65rem] uppercase tracking-wider text-jx-muted">
            {{ p.stock === 0 ? 'agotada' : p.stock < LOW_STOCK ? 'stock bajo' : 'en stock' }}
          </p>
        </div>

        <div class="flex w-full flex-wrap gap-2 sm:w-auto sm:flex-nowrap">
          <button v-if="allowed('VENTA_TIENDA')" type="button" :disabled="busy[p.id] || p.stock === 0"
            class="flex-1 rounded-full bg-jx-charcoal px-4 py-2 text-sm font-medium text-jx-white transition hover:bg-jx-charcoal-soft disabled:opacity-30 sm:flex-none"
            :aria-label="`Registrar venta de 1 ${p.name}`" @click="quick(p, 'VENTA_TIENDA')">
            −1 venta
          </button>
          <button v-if="allowed('INGRESO')" type="button" :disabled="busy[p.id]"
            class="flex-1 rounded-full border border-jx-charcoal px-4 py-2 text-sm font-medium transition hover:bg-jx-crudo disabled:opacity-30 sm:flex-none"
            :aria-label="`Registrar ingreso de 1 ${p.name}`" @click="quick(p, 'INGRESO')">
            +1 ingreso
          </button>
          <button type="button" class="rounded-full border border-jx-natural px-4 py-2 text-sm transition hover:border-jx-charcoal"
            @click="dialog = { product: p }">
            Registrar…
          </button>
          <button type="button" class="rounded-full px-3 py-2 text-sm text-jx-muted underline underline-offset-4 hover:text-jx-charcoal"
            @click="openHistory(p)">
            Historial
          </button>
        </div>
      </li>
      <li v-if="visible.length === 0" class="rounded-2xl bg-jx-white p-8 text-center text-jx-muted">
        No hay prendas con esos filtros.
      </li>
    </ul>

    <MovementDialog v-if="dialog" :key="dialog.product.id" :product="dialog.product" :types="types"
      @saved="onSaved" @close="dialog = null" />

    <dialog v-if="history" ref="historyDialog" aria-labelledby="hist-title" @close="history = null"
      @click="(e) => e.target === historyDialog && historyDialog.close()"
      class="fixed inset-y-0 right-0 left-auto m-0 h-full max-h-none w-full max-w-md bg-jx-white p-0 text-jx-charcoal shadow-2xl backdrop:bg-jx-charcoal/50">
      <div class="flex h-full flex-col">
        <header class="flex items-start justify-between border-b border-jx-natural px-6 py-5">
          <div>
            <p class="text-xs uppercase tracking-widest text-jx-muted">Historial (kardex)</p>
            <h2 id="hist-title" class="font-display text-2xl font-semibold">{{ history.product.name }}</h2>
          </div>
          <button type="button" aria-label="Cerrar" class="rounded-full p-2 text-2xl leading-none text-jx-muted hover:bg-jx-crudo"
            @click="historyDialog.close()">×</button>
        </header>
        <div class="flex-1 overflow-y-auto px-6">
          <p v-if="history.loading" class="py-6 text-sm text-jx-muted">Cargando…</p>
          <MovementList v-else :movements="history.movements" />
        </div>
      </div>
    </dialog>
  </section>
</template>
