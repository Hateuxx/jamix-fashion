<script setup>
import { formatDateTime, formatDelta } from '../lib/format.js';

/** Ledger lines (newest first). `showProduct` adds the garment column for the shop-wide feed. */
defineProps({
  movements: { type: Array, required: true },
  showProduct: { type: Boolean, default: false },
});
</script>

<template>
  <p v-if="movements.length === 0" class="py-6 text-center text-sm text-jx-muted">Todavía no hay movimientos.</p>
  <ul v-else class="divide-y divide-jx-crudo">
    <li v-for="m in movements" :key="m.id" class="flex items-start gap-3 py-3 text-sm">
      <span class="mt-0.5 w-12 shrink-0 rounded-lg py-1 text-center font-semibold tabular-nums"
        :class="m.quantity > 0 ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'">
        {{ formatDelta(m.quantity) }}
      </span>
      <div class="min-w-0 flex-1">
        <p class="font-medium">
          <template v-if="showProduct">{{ m.productName }} · </template>{{ m.typeLabel }}
        </p>
        <p class="text-xs text-jx-muted">
          {{ formatDateTime(m.createdAt) }} · {{ m.username }}<template v-if="m.note"> · “{{ m.note }}”</template>
        </p>
      </div>
      <span class="shrink-0 text-xs text-jx-muted tabular-nums">quedan {{ m.stockAfter }}</span>
    </li>
  </ul>
</template>
