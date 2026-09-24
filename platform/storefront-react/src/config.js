/** Empty = same origin (Vite proxy in dev, gateway in production). Set VITE_CATALOG_API_URL to call another host. */
export const CATALOG_API_URL = (import.meta.env.VITE_CATALOG_API_URL ?? '').replace(/\/+$/, '');

export const WHATSAPP_NUMBER = '51912452389';

export const CATEGORIES = [
  { slug: 'all', label: 'Todo' },
  { slug: 'ella', label: 'Ella' },
  { slug: 'el', label: 'Él' },
  { slug: 'ninos', label: 'Niños' },
];
