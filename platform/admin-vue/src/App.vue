<script setup>
import { computed, ref, watch } from 'vue';

import ActivityView from './components/ActivityView.vue';
import ChangePassword from './components/ChangePassword.vue';
import LoginForm from './components/LoginForm.vue';
import StockView from './components/StockView.vue';
import TeamView from './components/TeamView.vue';
import { useAuth } from './composables/useAuth.js';

const { session, isAuthenticated, mustChangePassword, can, logout } = useAuth();

const tabs = computed(() => [
  { id: 'stock', label: 'Stock' },
  { id: 'activity', label: 'Actividad' },
  ...(can('OWNER') ? [{ id: 'team', label: 'Equipo' }] : []),
]);
const tab = ref('stock');
const changingPassword = ref(false);

// Back to the stock tab whenever someone else logs in on this tab.
watch(() => session.value?.username, () => {
  tab.value = 'stock';
  changingPassword.value = false;
});
</script>

<template>
  <LoginForm v-if="!isAuthenticated" />
  <ChangePassword v-else-if="mustChangePassword" required />
  <template v-else>
    <header class="sticky top-0 z-30 border-b border-jx-natural/70 bg-jx-white/95 backdrop-blur">
      <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <p class="flex items-baseline gap-2">
          <span class="font-display text-2xl font-bold">Jamix</span>
          <span class="text-xs font-medium uppercase tracking-[0.3em] text-jx-gold-dark">Stock</span>
        </p>
        <nav aria-label="Secciones" class="order-last flex w-full gap-1 sm:order-none sm:w-auto">
          <button v-for="t in tabs" :key="t.id" type="button" :aria-current="tab === t.id ? 'page' : undefined"
            class="flex-1 rounded-full px-4 py-2 text-sm font-medium transition sm:flex-none"
            :class="tab === t.id && !changingPassword ? 'bg-jx-charcoal text-jx-white' : 'hover:bg-jx-crudo'"
            @click="tab = t.id; changingPassword = false">
            {{ t.label }}
          </button>
        </nav>
        <div class="flex items-center gap-3 text-sm">
          <div class="text-right leading-tight">
            <p class="font-medium">{{ session.displayName }}</p>
            <p class="text-xs text-jx-muted">{{ session.roleLabel }}</p>
          </div>
          <details class="relative">
            <summary class="cursor-pointer list-none rounded-full border border-jx-natural px-3 py-1.5 hover:border-jx-charcoal">Cuenta</summary>
            <div class="absolute right-0 z-40 mt-2 flex w-48 flex-col rounded-xl bg-jx-white p-2 shadow-lg ring-1 ring-jx-natural">
              <button type="button" class="rounded-lg px-3 py-2 text-left hover:bg-jx-crudo" @click="changingPassword = true">
                Cambiar contraseña
              </button>
              <button type="button" class="rounded-lg px-3 py-2 text-left hover:bg-jx-crudo" @click="logout()">
                Cerrar sesión
              </button>
            </div>
          </details>
        </div>
      </div>
    </header>
    <main>
      <ChangePassword v-if="changingPassword" @done="changingPassword = false" />
      <StockView v-else-if="tab === 'stock'" />
      <ActivityView v-else-if="tab === 'activity'" />
      <TeamView v-else-if="tab === 'team'" />
    </main>
  </template>
</template>
