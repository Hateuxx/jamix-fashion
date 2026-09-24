import { randomUUID } from 'node:crypto';

import cors from 'cors';
import express from 'express';
import rateLimit from 'express-rate-limit';
import helmet from 'helmet';

import { buildWhatsAppUrl, isSpam, validateLead } from './lead.js';

/**
 * POST /api/leads accepts two kinds of client:
 *  - the SSR page's plain <form> (urlencoded, works without JS): answered with a 303 redirect to
 *    WhatsApp, or back to the form with ?lead=error;
 *  - fetch() clients such as the React storefront (JSON): answered with JSON.
 */
export function createApp({ config, notifier, logger = console, now = () => new Date() }) {
  const app = express();
  app.disable('x-powered-by');
  app.set('trust proxy', config.trustProxy);

  app.use(helmet());
  app.use(express.json({ limit: '10kb' }));
  app.use(express.urlencoded({ extended: false, limit: '10kb' }));

  app.get('/health', (_req, res) => res.json({ status: 'ok' }));

  const leadsLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: 10,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    message: { error: 'Demasiadas solicitudes. Escríbenos directo por WhatsApp.' },
  });

  const corsForApi = cors({ origin: config.corsOrigins, methods: ['POST'] });

  app.options('/api/leads', corsForApi);
  app.post('/api/leads', corsForApi, leadsLimiter, (req, res) => {
    const fromHtmlForm = req.is('application/x-www-form-urlencoded') === 'application/x-www-form-urlencoded';
    const backToForm = `${config.siteUrl}/?lead=error#contacto`;

    // Bots get a success-looking answer and nothing is sent anywhere.
    if (isSpam(req.body)) {
      return fromHtmlForm ? res.redirect(303, config.siteUrl) : res.status(202).json({ ok: true });
    }

    const result = validateLead(req.body, { now: now() });
    if (!result.ok) {
      return fromHtmlForm ? res.redirect(303, backToForm) : res.status(422).json({ errors: result.errors });
    }

    const id = randomUUID();
    const reference = id.slice(0, 8);
    const whatsappUrl = buildWhatsAppUrl(result.lead, config.whatsappNumber, reference);

    // Fire and forget: a slow or failing webhook must never cost us the customer's WhatsApp chat.
    Promise.resolve()
      .then(() => notifier.notify({ id, reference, receivedAt: now().toISOString(), ...result.lead }))
      .catch((err) => logger.error(`[lead] notification failed for ${id}: ${err.message}`));

    return fromHtmlForm
      ? res.redirect(303, whatsappUrl)
      : res.status(201).json({ id, reference, whatsappUrl });
  });

  app.use((_req, res) => res.status(404).json({ error: 'Not found' }));

  // Express 5 forwards body-parser errors (malformed JSON, oversized payloads) here.
  // eslint-disable-next-line no-unused-vars
  app.use((err, _req, res, _next) => {
    const status = Number.isInteger(err.status) && err.status >= 400 && err.status < 500 ? err.status : 500;
    if (status === 500) logger.error(err);
    res.status(status).json({ error: status === 500 ? 'Error interno' : 'Solicitud inválida' });
  });

  return app;
}
