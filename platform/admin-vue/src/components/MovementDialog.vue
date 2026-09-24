<script setup>
import { computed, onMounted, ref } from 'vue';

import { useAuth } from '../composables/useAuth.js';

/** Registers a stock movement (or a physical count) for one product. */
const props = defineProps({
  product: { type: Object, required: true },
  types: { type: Array, required: true }, // from GET /movement-types, with `allowed` per role
  initialType: { type: String, default: '' },
});
const emit = defineEmits(['saved', 'close']);

const { api, can } = useAuth();
const dialog = ref(null);
const COUNT = 'CONTEO';

const options = computed(() => {
  const list = props.types.filter((t) => t.allowed);
  if (can('MANAGER')) list.push({ type: COUNT, label: 'Conteo físico (corregir el total)', sign: 0, allowed: true });
  return list;
});

const type = ref(props.initialType || options.value[0]?.type || '');
const quantity = ref(1);
const counted = ref(props.product.stock);
const note = ref('');
const error = ref('');
const busy = ref(false);

const selected = computed(() => options.value.find((o) => o.type === type.value));
const isCount = computed(() => type.value === COUNT);
const after = computed(() =>
  isCount.value ? Number(counted.value) : props.product.stock + (selected.value?.sign ?? 0) * Number(quantity.value),
);
const invalid = computed(() => {
  if (!selected.value) return 'Elige un tipo de movimiento.';
  if (isCount.value) return Number.isInteger(Number(counted.value)) && counted.value >= 0 ? '' : 'Indica el stock contado.';
  if (!Number.isInteger(Number(quantity.value)) || quantity.value < 1) return 'La cantidad debe ser 1 o más.';
  if (after.value < 0) return `No alcanza: solo hay ${props.product.stock}.`;
  return '';
});

onMounted(() => dialog.value?.showModal());

async function submit() {
  if (invalid.value) return;
  error.value = '';
  busy.value = true;
  try {
    let product;
    if (isCount.value) {
      product = await api(`/api/v1/admin/products/${props.product.id}/stock`, {
        method: 'PATCH',
        body: { stock: Number(counted.value), version: props.product.version, note: note.value || null },
      });
    } else {
      ({ product } = await api(`/api/v1/admin/products/${props.product.id}/movements`, {
        method: 'POST',
        body: { type: type.value, quantity: Number(quantity.value), note: note.value || null },
      }));
    }
    emit('saved', { product, label: selected.value.label });
    dialog.value?.close();
  } catch (e) {
    error.value = e.status === 409 ? `${e.message} Cierra y vuelve a intentarlo con los datos actualizados.` : e.message;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <dialog ref="dialog" aria-labelledby="mv-title" @close="emit('close')"
    @click="(e) => e.target === dialog && dialog.close()"
    class="m-auto w-[min(32rem,calc(100%-2rem))] rounded-3xl bg-jx-white p-0 text-jx-charcoal shadow-2xl backdrop:bg-jx-charcoal/50">
    <form class="flex flex-col gap-5 p-6 sm:p-8" @submit.prevent="submit">
      <div class="flex items-start justify-between gap-4">
        <div>
          <p class="text-xs uppercase tracking-widest text-jx-muted">Registrar movimiento</p>
          <h2 id="mv-title" class="font-display text-2xl font-semibold">{{ product.name }}</h2>
          <p class="text-sm text-jx-muted">Stock actual: <strong class="text-jx-charcoal">{{ product.stock }}</strong></p>
        </div>
        <button type="button" aria-label="Cerrar" class="rounded-full p-2 text-2xl leading-none text-jx-muted hover:bg-jx-crudo"
          @click="dialog.close()">×</button>
      </div>

      <fieldset class="flex flex-col gap-2">
        <legend class="mb-2 text-sm font-medium">¿Qué pasó?</legend>
        <label v-for="o in options" :key="o.type"
          class="flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 text-sm transition"
          :class="type === o.type ? 'border-jx-charcoal bg-jx-crudo' : 'border-jx-natural hover:border-jx-charcoal'">
          <input v-model="type" type="radio" name="type" :value="o.type" class="accent-jx-charcoal" />
          <span class="flex-1">{{ o.label }}</span>
          <span class="text-xs font-semibold" :class="o.sign > 0 ? 'text-green-700' : o.sign < 0 ? 'text-red-700' : 'text-jx-muted'">
            {{ o.sign > 0 ? 'Entra' : o.sign < 0 ? 'Sale' : 'Total' }}
          </span>
        </label>
      </fieldset>

      <label v-if="isCount" class="flex flex-col gap-1.5 text-sm font-medium">
        ¿Cuántas hay realmente en el estante?
        <input v-model.number="counted" type="number" min="0" step="1" inputmode="numeric" required
          class="w-32 rounded-xl border border-jx-natural px-4 py-3 text-lg tabular-nums outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
      </label>
      <label v-else class="flex flex-col gap-1.5 text-sm font-medium">
        Cantidad
        <input v-model.number="quantity" type="number" min="1" max="999" step="1" inputmode="numeric" required
          class="w-32 rounded-xl border border-jx-natural px-4 py-3 text-lg tabular-nums outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
      </label>

      <label class="flex flex-col gap-1.5 text-sm font-medium">
        Nota <span class="font-normal text-jx-muted">(opcional: cliente, proveedor, motivo…)</span>
        <input v-model="note" maxlength="200"
          class="rounded-xl border border-jx-natural px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
      </label>

      <p class="rounded-xl bg-jx-crudo px-4 py-3 text-sm" aria-live="polite">
        Stock: <strong>{{ product.stock }}</strong> → <strong :class="after < 0 ? 'text-red-700' : ''">{{ Number.isFinite(after) ? after : '?' }}</strong>
      </p>
      <p v-if="invalid && (quantity !== '' || isCount)" class="text-sm text-red-700">{{ invalid }}</p>
      <p v-if="error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800">{{ error }}</p>

      <button type="submit" :disabled="busy || Boolean(invalid)"
        class="rounded-full bg-jx-charcoal px-6 py-3 text-sm font-semibold text-jx-white transition hover:bg-jx-charcoal-soft disabled:opacity-40">
        {{ busy ? 'Guardando…' : 'Registrar' }}
      </button>
    </form>
  </dialog>
</template>
