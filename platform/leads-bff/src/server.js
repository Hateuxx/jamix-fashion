import { createApp } from './app.js';
import { loadConfig } from './config.js';
import { createNotifier } from './notifier.js';

const config = loadConfig();
const app = createApp({ config, notifier: createNotifier({ webhookUrl: config.webhookUrl }) });

const server = app.listen(config.port, () => {
  console.log(`Jamix leads BFF listening on http://localhost:${config.port}`);
});

const shutdown = () => server.close(() => process.exit(0));
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
