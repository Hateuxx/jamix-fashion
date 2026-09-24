<script setup>
import { onMounted, reactive, ref } from 'vue';

import { useAuth } from '../composables/useAuth.js';
import { formatDateTime, temporaryPassword } from '../lib/format.js';

/** Team accounts (OWNER only): one account per person, with a role. */
const { api, session } = useAuth();

const ROLES = [
  { value: 'SELLER', label: 'Vendedor(a)', help: 'Ve el stock y registra ventas.' },
  { value: 'MANAGER', label: 'Encargado(a)', help: 'Además: ingresos, devoluciones, mermas, conteos y productos.' },
  { value: 'OWNER', label: 'Dueño(a)', help: 'Además: gestiona el equipo.' },
];

const team = ref([]);
const status = ref('loading');
const message = ref({ text: '', tone: 'info' });
const handover = ref(null); // { name, username, password } shown once after create/reset
const form = reactive({ username: '', displayName: '', role: 'SELLER' });
const creating = ref(false);

function show(text, tone = 'info') {
  message.value = { text, tone };
}

async function load() {
  try {
    team.value = await api('/api/v1/admin/users');
    status.value = 'ready';
  } catch (e) {
    status.value = 'error';
    show(e.message, 'error');
  }
}

async function create() {
  creating.value = true;
  const password = temporaryPassword();
  try {
    const user = await api('/api/v1/admin/users', {
      method: 'POST',
      body: { username: form.username.trim(), displayName: form.displayName.trim(), role: form.role, temporaryPassword: password },
    });
    handover.value = { name: user.displayName, username: user.username, password };
    Object.assign(form, { username: '', displayName: '', role: 'SELLER' });
    show('');
    await load();
  } catch (e) {
    show(e.message, 'error');
  } finally {
    creating.value = false;
  }
}

async function update(user, patch, confirmText) {
  if (confirmText && !window.confirm(confirmText)) {
    await load(); // undo the select change in the UI
    return;
  }
  try {
    await api(`/api/v1/admin/users/${user.id}`, { method: 'PATCH', body: patch });
    show(`${user.displayName}: cambios guardados.`, 'ok');
  } catch (e) {
    show(e.message, 'error');
  }
  await load();
}

async function reset(user) {
  if (!window.confirm(`¿Restablecer la contraseña de ${user.displayName}? Se cerrará su sesión.`)) return;
  const password = temporaryPassword();
  try {
    await api(`/api/v1/admin/users/${user.id}/reset-password`, { method: 'POST', body: { temporaryPassword: password } });
    handover.value = { name: user.displayName, username: user.username, password };
    await load();
  } catch (e) {
    show(e.message, 'error');
  }
}

async function copy(text) {
  try {
    await navigator.clipboard.writeText(text);
    show('Copiado.', 'ok');
  } catch {
    show('No se pudo copiar: selecciónalo y cópialo a mano.', 'error');
  }
}

onMounted(load);
</script>

<template>
  <section class="mx-auto max-w-4xl px-4 py-8 sm:px-6">
    <h1 class="font-display text-2xl font-semibold">Equipo</h1>
    <p class="mb-6 text-sm text-jx-muted">Una cuenta por persona: así el historial muestra quién hizo cada cambio.</p>

    <!-- One-time handover of a temporary password -->
    <div v-if="handover" role="status" class="mb-6 rounded-2xl border-2 border-jx-gold bg-jx-white p-5">
      <p class="font-medium">Entrega estos datos a {{ handover.name }}:</p>
      <dl class="mt-3 grid grid-cols-[auto_1fr_auto] items-center gap-x-4 gap-y-2 text-sm">
        <dt class="text-jx-muted">Usuario</dt>
        <dd class="font-mono">{{ handover.username }}</dd>
        <dd><button type="button" class="underline underline-offset-4" @click="copy(handover.username)">Copiar</button></dd>
        <dt class="text-jx-muted">Contraseña temporal</dt>
        <dd class="font-mono">{{ handover.password }}</dd>
        <dd><button type="button" class="underline underline-offset-4" @click="copy(handover.password)">Copiar</button></dd>
      </dl>
      <p class="mt-3 text-xs text-jx-muted">
        Solo se muestra ahora. Al ingresar, el sistema le pedirá elegir su propia contraseña.
      </p>
      <button type="button" class="mt-3 text-sm font-medium underline underline-offset-4" @click="handover = null">Listo, ya la entregué</button>
    </div>

    <p v-if="message.text" role="status" class="mb-4 rounded-xl px-4 py-3 text-sm"
      :class="message.tone === 'error' ? 'bg-red-50 text-red-800' : 'bg-green-50 text-green-900'">{{ message.text }}</p>

    <!-- New account -->
    <form class="mb-8 grid gap-3 rounded-2xl bg-jx-white p-5 sm:grid-cols-[1fr_1fr_auto_auto] sm:items-end" @submit.prevent="create">
      <label class="flex flex-col gap-1 text-sm font-medium">
        Nombre
        <input v-model="form.displayName" required maxlength="80" placeholder="Rosa Quispe"
          class="rounded-xl border border-jx-natural px-3 py-2 font-normal outline-none focus:border-jx-charcoal" />
      </label>
      <label class="flex flex-col gap-1 text-sm font-medium">
        Usuario
        <input v-model="form.username" required pattern="[a-zA-Z0-9._\-]{3,40}" autocapitalize="none" placeholder="rosa.q"
          title="3 a 40 letras, números, punto o guion"
          class="rounded-xl border border-jx-natural px-3 py-2 font-normal outline-none focus:border-jx-charcoal" />
      </label>
      <label class="flex flex-col gap-1 text-sm font-medium">
        Rol
        <select v-model="form.role" class="rounded-xl border border-jx-natural bg-jx-white px-3 py-2 font-normal">
          <option v-for="r in ROLES" :key="r.value" :value="r.value">{{ r.label }}</option>
        </select>
      </label>
      <button type="submit" :disabled="creating"
        class="rounded-full bg-jx-charcoal px-5 py-2.5 text-sm font-semibold text-jx-white hover:bg-jx-charcoal-soft disabled:opacity-50">
        Crear cuenta
      </button>
      <p class="text-xs text-jx-muted sm:col-span-4">{{ ROLES.find((r) => r.value === form.role).help }}</p>
    </form>

    <p v-if="status === 'loading'" class="text-jx-muted">Cargando equipo…</p>
    <ul v-else class="flex flex-col gap-2">
      <li v-for="u in team.filter((t) => t.role !== 'CUSTOMER')" :key="u.id"
        class="flex flex-wrap items-center gap-3 rounded-2xl bg-jx-white p-4" :class="{ 'opacity-60': !u.active }">
        <div class="min-w-0 flex-1">
          <p class="font-medium">
            {{ u.displayName }}
            <span v-if="u.username === session.username" class="text-xs text-jx-muted">(tú)</span>
          </p>
          <p class="text-xs text-jx-muted">
            {{ u.username }} · desde {{ formatDateTime(u.createdAt) }}
            <template v-if="!u.active"> · <strong>desactivada</strong></template>
            <template v-else-if="u.mustChangePassword"> · pendiente de elegir contraseña</template>
          </p>
        </div>
        <select :value="u.role" :disabled="u.username === session.username" :aria-label="`Rol de ${u.displayName}`"
          class="rounded-full border border-jx-natural bg-jx-white px-3 py-1.5 text-sm disabled:opacity-50"
          @change="(e) => update(u, { role: e.target.value }, `¿Cambiar el rol de ${u.displayName}? Se cerrará su sesión.`)">
          <option v-for="r in ROLES" :key="r.value" :value="r.value">{{ r.label }}</option>
        </select>
        <template v-if="u.username !== session.username">
          <button type="button" class="rounded-full border border-jx-natural px-3 py-1.5 text-sm hover:border-jx-charcoal"
            @click="reset(u)">Restablecer contraseña</button>
          <button type="button" class="rounded-full px-3 py-1.5 text-sm"
            :class="u.active ? 'text-red-700 hover:bg-red-50' : 'text-green-800 hover:bg-green-50'"
            @click="update(u, { active: !u.active }, u.active ? `¿Desactivar a ${u.displayName}? No podrá ingresar y su sesión se cerrará ya.` : null)">
            {{ u.active ? 'Desactivar' : 'Reactivar' }}
          </button>
        </template>
      </li>
    </ul>
  </section>
</template>
