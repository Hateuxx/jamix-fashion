const list = (value) =>
  (value ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

export function loadConfig(env = process.env) {
  return {
    port: Number(env.PORT ?? 4000),
    siteUrl: (env.SITE_URL ?? 'http://localhost:8080').replace(/\/+$/, ''),
    corsOrigins: list(env.CORS_ORIGINS ?? 'http://localhost:8080,http://localhost:5173'),
    whatsappNumber: env.WHATSAPP_NUMBER ?? '51912452389',
    webhookUrl: env.LEADS_WEBHOOK_URL || null,
    trustProxy: Number(env.TRUST_PROXY ?? 0),
  };
}
