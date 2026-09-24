<script setup>
import { computed, onMounted, ref } from 'vue';

import { useAuth } from '../composables/useAuth.js';
import MovementList from './MovementList.vue';

/** Shop-wide ledger: the latest stock movements by everyone, filterable by person and kind. */
const { api } = useAuth();
const movements = ref([]);
const status = ref('loading');
const error = ref('');
const person = ref('all');
const kind = ref('all');

const people = computed(() => [...new Set(movements.value.map((m) => m.username))].sort());
const kinds = computed(() => [...new Map(movements.value.map((m) => [m.type, m.typeLabel])).entries()]);
const visible = computed(() =>
  movements.value.filter((m) => (person.value === 'all' || m.username === person.value) && (kind.value === 'all' || m.type === kind.value)),
);
const unitsOut = computed(() => visible.value.filter((m) => m.quantity < 0).reduce((s, m) => s - m.quantity, 0));
const unitsIn = computed(() => visible.value.filter((m) => m.quantity > 0).reduce((s, m) => s + m.quantity, 0));

async function load() {
  status.value = 'loading';
  try {
    movements.value = await api('/api/v1/admin/movements?limit=200');
    status.value = 'ready';
  } catch (e) {
    error.value = e.message;
    status.value = 'error';
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-4xl px-4 py-8 sm:px-6">
    <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="font-display text-2xl font-semibold">Actividad</h1>
        <p class="text-sm text-jx-muted">Últimos 200 movimientos de stock: quién, cuándo y por qué.</p>
      </div>
      <button type="button" class="rounded-full border border-jx-charcoal px-4 py-2 text-sm hover:bg-jx-crudo" @click="load">
        Actualizar
      </button>
    </div>

    <div class="mb-4 flex flex-wrap gap-3 rounded-2xl bg-jx-white p-4">
      <select v-model="person" aria-label="Persona" class="rounded-full border border-jx-natural bg-jx-white px-4 py-2 text-sm">
        <option value="all">Todas las personas</option>
        <option v-for="p in people" :key="p" :value="p">{{ p }}</option>
      </select>
      <select v-model="kind" aria-label="Tipo de movimiento" class="rounded-full border border-jx-natural bg-jx-white px-4 py-2 text-sm">
        <option value="all">Todos los movimientos</option>
        <option v-for="[type, label] in kinds" :key="type" :value="type">{{ label }}</option>
      </select>
      <p class="ml-auto self-center text-sm text-jx-muted">
        Entraron <strong class="text-green-800">{{ unitsIn }}</strong> · salieron <strong class="text-red-800">{{ unitsOut }}</strong>
      </p>
    </div>

    <p v-if="status === 'loading'" class="text-jx-muted">Cargando…</p>
    <p v-else-if="status === 'error'" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800">{{ error }}</p>
    <div v-else class="rounded-2xl bg-jx-white px-4 sm:px-6">
      <MovementList :movements="visible" show-product />
    </div>
  </section>
</template>
