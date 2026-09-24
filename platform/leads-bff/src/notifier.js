/**
 * Tells the business about a new lead. Without a webhook it only logs; with LEADS_WEBHOOK_URL it
 * POSTs the lead as JSON, which lets an automation (n8n, Zapier, a WhatsApp Cloud API adapter)
 * push it to the owner's phone or a CRM without redeploying this service.
 */
export function createNotifier({ webhookUrl, logger = console, fetchImpl = fetch, timeoutMs = 5000 }) {
  return {
    async notify(lead) {
      if (!webhookUrl) {
        logger.info(`[lead] ${lead.id} ${lead.eventType} ${lead.name}`);
        return;
      }
      const res = await fetchImpl(webhookUrl, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify(lead),
        signal: AbortSignal.timeout(timeoutMs),
      });
      if (!res.ok) throw new Error(`Lead webhook answered ${res.status}`);
    },
  };
}
