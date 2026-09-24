<script setup>
import { computed, ref } from 'vue';

import { useAuth } from '../composables/useAuth.js';

const props = defineProps({ required: { type: Boolean, default: false } });
const emit = defineEmits(['done']);

const { session, api, applySession, logout } = useAuth();
const current = ref('');
const next = ref('');
const repeat = ref('');
const error = ref('');
const busy = ref(false);

const MIN = 8;
const problem = computed(() => {
  if (next.value && next.value.length < MIN) return `Usa al menos ${MIN} caracteres (una frase corta es ideal).`;
  if (repeat.value && next.value !== repeat.value) return 'Las contraseñas nuevas no coinciden.';
  return '';
});

async function submit() {
  if (problem.value) return;
  error.value = '';
  busy.value = true;
  try {
    const fresh = await api('/api/v1/me/password', {
      method: 'PUT',
      body: { currentPassword: current.value, newPassword: next.value },
    });
    applySession(fresh); // the old token no longer works
    emit('done');
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>

<template>
  <form class="mx-auto mt-10 flex w-[calc(100%-2rem)] max-w-md flex-col gap-5 rounded-3xl bg-jx-white p-8 shadow-sm motion-safe:animate-fade-in"
    @submit.prevent="submit">
    <div>
      <h1 class="font-display text-2xl font-semibold">
        {{ props.required ? `Hola, ${session.displayName}` : 'Cambiar mi contraseña' }}
      </h1>
      <p v-if="props.required" class="mt-1 text-sm text-jx-charcoal-soft">
        Tu cuenta tiene una contraseña temporal. Elige una propia para empezar; nadie más la conocerá.
      </p>
    </div>

    <p v-if="error" role="alert" class="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-800">{{ error }}</p>

    <label class="flex flex-col gap-1.5 text-sm font-medium">
      {{ props.required ? 'Contraseña temporal' : 'Contraseña actual' }}
      <input v-model="current" type="password" required autocomplete="current-password"
        class="rounded-xl border border-jx-natural px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
    </label>
    <label class="flex flex-col gap-1.5 text-sm font-medium">
      Nueva contraseña
      <input v-model="next" type="password" required :minlength="MIN" autocomplete="new-password"
        class="rounded-xl border border-jx-natural px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
    </label>
    <label class="flex flex-col gap-1.5 text-sm font-medium">
      Repite la nueva contraseña
      <input v-model="repeat" type="password" required autocomplete="new-password"
        class="rounded-xl border border-jx-natural px-4 py-3 font-normal outline-none focus:border-jx-charcoal focus:ring-2 focus:ring-jx-beige" />
    </label>
    <p v-if="problem" class="text-sm text-red-700">{{ problem }}</p>

    <div class="flex flex-col gap-2">
      <button type="submit" :disabled="busy || Boolean(problem)"
        class="rounded-full bg-jx-charcoal px-6 py-3 text-sm font-semibold text-jx-white transition hover:bg-jx-charcoal-soft disabled:opacity-50">
        {{ busy ? 'Guardando…' : 'Guardar contraseña' }}
      </button>
      <button v-if="props.required" type="button" class="text-sm text-jx-muted underline underline-offset-4" @click="logout()">
        Salir
      </button>
      <button v-else type="button" class="text-sm text-jx-muted underline underline-offset-4" @click="emit('done')">
        Cancelar
      </button>
    </div>
  </form>
</template>
