export const EVENT_TYPES = {
  bautizo: 'Bautizo',
  'baby-shower': 'Baby shower',
  'gender-reveal': 'Gender reveal',
  'book-familiar': 'Book familiar',
  otro: 'Otro',
};

// Control characters (except newline/tab) have no business in a WhatsApp message.
const CONTROL_CHARS = /[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]/g;

const text = (value) => (typeof value === 'string' ? value.replace(CONTROL_CHARS, '').trim() : '');

/** Today's date in Lima as YYYY-MM-DD. Using UTC would reject "today" after 7 p.m. local time. */
export function limaToday(now = new Date()) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(now);
}

function isRealDate(iso) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return false;
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  return date.getUTCFullYear() === y && date.getUTCMonth() === m - 1 && date.getUTCDate() === d;
}

/**
 * Validates and normalizes the "Cuéntanos el evento" form. Works for both JSON bodies and
 * urlencoded form posts, where every field arrives as a (possibly empty) string.
 */
export function validateLead(input, { now = new Date() } = {}) {
  const body = input && typeof input === 'object' ? input : {};
  const errors = {};

  const name = text(body.name);
  if (name.length < 2 || name.length > 80) errors.name = 'Escribe tu nombre (2 a 80 caracteres).';

  const rawPhone = text(body.phone);
  const phone = rawPhone.replace(/[\s\-().]/g, '').replace(/^\+/, '');
  if (rawPhone && !/^\d{9,15}$/.test(phone)) errors.phone = 'El número debe tener entre 9 y 15 dígitos.';

  const eventType = text(body.eventType);
  if (!Object.hasOwn(EVENT_TYPES, eventType)) errors.eventType = 'Elige el tipo de evento.';

  const eventDate = text(body.eventDate);
  if (eventDate) {
    if (!isRealDate(eventDate)) errors.eventDate = 'La fecha no es válida.';
    else if (eventDate < limaToday(now)) errors.eventDate = 'La fecha del evento ya pasó.';
  }

  const rawPeople = text(String(body.people ?? ''));
  const people = rawPeople ? Number(rawPeople) : undefined;
  if (people !== undefined && (!Number.isInteger(people) || people < 1 || people > 30)) {
    errors.people = 'Indica entre 1 y 30 personas.';
  }

  const message = text(body.message);
  if (message.length > 1000) errors.message = 'El mensaje puede tener hasta 1000 caracteres.';

  if (Object.keys(errors).length > 0) return { ok: false, errors };

  return {
    ok: true,
    lead: {
      name,
      phone: phone || undefined,
      eventType,
      eventDate: eventDate || undefined,
      people,
      message: message || undefined,
    },
  };
}

/** A honeypot field that only bots fill in. */
export const isSpam = (body) => Boolean(body && typeof body === 'object' && text(body.website));

export function buildWhatsAppUrl(lead, number, reference) {
  const lines = [
    `Hola Jamix Fashion, soy ${lead.name}. Quiero que me armen el outfit.`,
    `Evento: ${EVENT_TYPES[lead.eventType]}`,
  ];
  if (lead.eventDate) {
    const [y, m, d] = lead.eventDate.split('-');
    lines.push(`Fecha: ${d}/${m}/${y}`);
  }
  if (lead.people) lines.push(`Personas: ${lead.people}`);
  if (lead.phone) lines.push(`Mi WhatsApp: +${lead.phone}`);
  if (lead.message) lines.push('', lead.message);
  if (reference) lines.push('', `Ref: ${reference}`);

  return `https://wa.me/${number}?text=${encodeURIComponent(lines.join('\n'))}`;
}
