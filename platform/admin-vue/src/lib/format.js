const dateTime = new Intl.DateTimeFormat('es-PE', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Lima' });

export const formatDateTime = (iso) => (iso ? dateTime.format(new Date(iso)) : '');

/** "+3" / "−2" with a real minus sign. */
export const formatDelta = (n) => (n > 0 ? `+${n}` : `−${Math.abs(n)}`);

export const CATEGORY_LABELS = { ELLA: 'Ella', EL: 'Él', NINOS: 'Niños' };

export const LOW_STOCK = 5;

/** Lower-case and accent-free, so "tunica" finds "Túnica". */
export const normalize = (text) =>
  (text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

/** Readable temporary password for a new account or a reset: e.g. "Jamix-7kq2-M9xh". */
export function temporaryPassword() {
  const alphabet = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // no 0/O/1/l/I
  const bytes = crypto.getRandomValues(new Uint8Array(8));
  const chars = [...bytes].map((b) => alphabet[b % alphabet.length]).join('');
  return `Jamix-${chars.slice(0, 4)}-${chars.slice(4)}`;
}
