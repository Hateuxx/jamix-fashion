<script setup>
import { ref } from 'vue';

import { useAuth } from '../composables/useAuth.js';

const { login, notice } = useAuth();
const username = ref('');
const password = ref('');
const error = ref('');
const busy = ref(false);

async function submit() {
  error.value = '';
  busy.value = true;
  try {
    await login(username.value.trim(), password.value);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
    password.value = '';
  }
}
</script>

<template>
  <form
    class="mx-auto mt-16 flex w-[calc(100%-2rem)] max-w-sm flex-col gap-5 rounded-3xl bg-jx-white p-8 shadow-sm motion-safe:animate-fade-in sm:mt-24"
    @submit.prevent="submit"
  >
    <div>
      <p class="font-display text-3xl font-bold">Jamix</p>
      <p class="text-sm text-jx-muted">Panel de stock · ingresa con tu usuario personal</p>
    </div>

    <p v-if="notice && !error" role="status" class="rounded-xl bg-jx-crudo px-4 py-3 text-sm">{{ notice }}</p>
    <p v-if="error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800">{{ error }}</p>

    <label class="flex flex-col gap-1.5 text-sm font-medium">
      Usuario
      <input v-model="username" required autocomplete="username" autocapitalize="none"
        class="rounded-xl border border-jx-natural px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
    </label>
    <label class="flex flex-col gap-1.5 text-sm font-medium">
      Contraseña
      <input v-model="password" type="password" required autocomplete="current-password"
        class="rounded-xl border border-jx-natural px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
    </label>
    <button type="submit" :disabled="busy"
      class="rounded-full bg-jx-charcoal px-6 py-3 text-sm font-semibold text-jx-white transition hover:bg-jx-charcoal-soft disabled:opacity-60">
      {{ busy ? 'Ingresando…' : 'Ingresar' }}
    </button>
  </form>
</template>
