import { computed, ref } from 'vue';

import { apiFetch } from '../api/client.js';

const STORAGE_KEY = 'jamix.admin.session';
const ROLE_LEVEL = { SELLER: 1, MANAGER: 2, OWNER: 3 };

// sessionStorage keeps the session per tab and drops it when the tab closes. Moving the token into an
// httpOnly cookie set by a BFF is the next hardening step (it takes XSS out of the picture).
function readStored() {
  try {
    return JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? 'null');
  } catch {
    return null;
  }
}

function writeStored(value) {
  try {
    if (value) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    else sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage blocked (private mode, policy): the session just won't survive a reload.
  }
}

function expiresAt(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.exp * 1000;
  } catch {
    return 0;
  }
}

const session = ref(readStored());
if (session.value && expiresAt(session.value.accessToken) <= Date.now()) {
  session.value = null;
  writeStored(null);
}

const notice = ref(''); // shown on the login screen (e.g. "tu sesión se cerró")
const MAX_TIMEOUT = 2 ** 31 - 1; // setTimeout fires immediately above ~24.8 days

let expiryTimer;
function scheduleExpiry() {
  clearTimeout(expiryTimer);
  if (!session.value) return;
  const delay = Math.max(0, expiresAt(session.value.accessToken) - Date.now());
  expiryTimer = setTimeout(() => logout('Tu sesión expiró. Vuelve a ingresar.'), Math.min(delay, MAX_TIMEOUT));
}

function applySession(next) {
  session.value = next;
  writeStored(next);
  scheduleExpiry();
}

function logout(message = '') {
  notice.value = message;
  session.value = null;
  writeStored(null);
  clearTimeout(expiryTimer);
}

async function login(username, password) {
  const result = await apiFetch('/api/v1/auth/login', { method: 'POST', body: { username, password } });
  if (!ROLE_LEVEL[result.role]) {
    throw new Error('Esta cuenta no tiene acceso al panel de stock.');
  }
  notice.value = '';
  applySession(result);
}

/** API call with the current token. A 401 means the account was deactivated/reset or the session expired. */
async function api(path, options = {}) {
  try {
    return await apiFetch(path, { ...options, token: session.value?.accessToken });
  } catch (e) {
    if (e.status === 401) logout('Tu sesión se cerró. Vuelve a ingresar.');
    throw e;
  }
}

scheduleExpiry();

/** Shared auth state (module-level singleton, so every component sees the same session). */
export function useAuth() {
  return {
    session,
    notice,
    isAuthenticated: computed(() => Boolean(session.value)),
    mustChangePassword: computed(() => Boolean(session.value?.mustChangePassword)),
    /** True if the current role is at least `min` (SELLER < MANAGER < OWNER). */
    can: (min) => (ROLE_LEVEL[session.value?.role] ?? 0) >= ROLE_LEVEL[min],
    login,
    logout,
    applySession,
    api,
  };
}
