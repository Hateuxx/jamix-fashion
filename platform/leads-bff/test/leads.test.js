import assert from 'node:assert/strict';
import { after, before, beforeEach, describe, it } from 'node:test';

import { createApp } from '../src/app.js';
import { limaToday, validateLead } from '../src/lead.js';

const config = {
  port: 0,
  siteUrl: 'http://localhost:8080',
  corsOrigins: ['http://localhost:8080'],
  whatsappNumber: '51912452389',
  webhookUrl: null,
  trustProxy: 0,
};

// 2026-09-23 23:30 in Lima is already 2026-09-24 in UTC.
const NOW = new Date('2026-09-24T04:30:00Z');

const silentLogger = { info() {}, error() {} };
let notified = [];
let failNotifier = false;
const notifier = {
  async notify(lead) {
    if (failNotifier) throw new Error('webhook down');
    notified.push(lead);
  },
};

let server;
let baseUrl;

before(async () => {
  const app = createApp({ config, notifier, logger: silentLogger, now: () => NOW });
  await new Promise((resolve) => {
    server = app.listen(0, resolve);
  });
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(() => new Promise((resolve) => server.close(resolve)));

beforeEach(() => {
  notified = [];
  failNotifier = false;
});

const postJson = (body) =>
  fetch(`${baseUrl}/api/leads`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });

const postForm = (fields) =>
  fetch(`${baseUrl}/api/leads`, {
    method: 'POST',
    body: new URLSearchParams(fields),
    redirect: 'manual',
  });

const flush = () => new Promise((resolve) => setImmediate(resolve));

describe('POST /api/leads (JSON)', () => {
  it('returns a WhatsApp link with the event details', async () => {
    const res = await postJson({
      name: 'Rosa Quispe',
      phone: '+51 987-654-321',
      eventType: 'bautizo',
      eventDate: '2026-10-10',
      people: 4,
      message: 'Somos papá, mamá y dos niños.',
    });
    assert.equal(res.status, 201);
    const body = await res.json();
    assert.match(body.whatsappUrl, /^https:\/\/wa\.me\/51912452389\?text=/);
    const text = decodeURIComponent(body.whatsappUrl.split('text=')[1]);
    assert.match(text, /soy Rosa Quispe/);
    assert.match(text, /Evento: Bautizo/);
    assert.match(text, /Fecha: 10\/10\/2026/);
    assert.match(text, /Personas: 4/);
    assert.match(text, /Mi WhatsApp: \+51987654321/);
    assert.match(text, new RegExp(`Ref: ${body.reference}`));

    await flush();
    assert.equal(notified.length, 1);
    assert.equal(notified[0].id, body.id);
  });

  it('rejects invalid input with field errors', async () => {
    const res = await postJson({ name: 'R', eventType: 'boda', people: 99 });
    assert.equal(res.status, 422);
    const { errors } = await res.json();
    assert.deepEqual(Object.keys(errors).sort(), ['eventType', 'name', 'people']);
    await flush();
    assert.equal(notified.length, 0);
  });

  it('answers malformed JSON with 400, not 500', async () => {
    const res = await postJson('{"name":');
    assert.equal(res.status, 400);
  });

  it('still answers when the notification webhook fails', async () => {
    failNotifier = true;
    const res = await postJson({ name: 'Rosa', eventType: 'otro' });
    assert.equal(res.status, 201);
  });

  it('silently drops honeypot submissions', async () => {
    const res = await postJson({ name: 'Bot', eventType: 'otro', website: 'http://spam.example' });
    assert.equal(res.status, 202);
    await flush();
    assert.equal(notified.length, 0);
  });
});

describe('POST /api/leads (HTML form, no JavaScript)', () => {
  it('redirects a valid submission straight to WhatsApp', async () => {
    const res = await postForm({ name: 'Rosa', eventType: 'book-familiar', eventDate: '', people: '', website: '' });
    assert.equal(res.status, 303);
    assert.match(res.headers.get('location'), /^https:\/\/wa\.me\/51912452389\?text=/);
  });

  it('sends an invalid submission back to the form on the storefront', async () => {
    const res = await postForm({ name: '', eventType: '' });
    assert.equal(res.status, 303);
    assert.equal(res.headers.get('location'), 'http://localhost:8080/?lead=error#contacto');
  });
});

describe('validateLead dates', () => {
  it('uses Lima time: "today" is still valid late in the evening', () => {
    assert.equal(limaToday(NOW), '2026-09-23');
    assert.equal(validateLead({ name: 'Rosa', eventType: 'otro', eventDate: '2026-09-23' }, { now: NOW }).ok, true);
  });

  it('rejects past and impossible dates', () => {
    assert.ok(validateLead({ name: 'Rosa', eventType: 'otro', eventDate: '2026-09-22' }, { now: NOW }).errors.eventDate);
    assert.ok(validateLead({ name: 'Rosa', eventType: 'otro', eventDate: '2027-02-30' }, { now: NOW }).errors.eventDate);
  });
});
