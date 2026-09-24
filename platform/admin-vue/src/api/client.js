/** Empty = same origin (Vite proxy, gateway). On Vercel: the backend's public URL. */
export const BASE_URL = (import.meta.env.VITE_CATALOG_API_URL ?? '').replace(/\/+$/, '');

/** Product photos live on the backend (/img/...), so they need its URL when the panel is hosted elsewhere. */
export const assetUrl = (path) => (path ? `${BASE_URL}${path}` : '');

export class ApiError extends Error {
  constructor(status, detail) {
    super(detail || `Error ${status}`);
    this.status = status;
  }
}

/** fetch wrapper for the catalog service: JSON in/out, bearer token, problem+json errors. */
export async function apiFetch(path, { token, method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json, application/problem+json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new ApiError(res.status, data?.detail);
  return data;
}
