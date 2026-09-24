// One command for the whole platform, without extra dependencies.
//   node scripts/run-all.mjs install  -> npm install in every service (one after another)
//   node scripts/run-all.mjs dev      -> starts all four services, output prefixed by service
//   node scripts/run-all.mjs test     -> runs every test suite (one after another)
//   node scripts/run-all.mjs build    -> production builds (one after another)
import { spawn, spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import net from 'node:net';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const isWindows = process.platform === 'win32';

// `isOurs` recognizes a copy of the same service already running (e.g. started in another terminal),
// so `dev` can reuse it instead of crashing with "port already in use".
const SERVICES = [
  {
    name: 'catalog', dir: 'catalog-service', color: 33, port: 8080, url: 'http://localhost:8080',
    probe: 'http://localhost:8080/api/v1/products', isOurs: (body) => body.includes('"slug"'),
  },
  {
    name: 'leads', dir: 'leads-bff', color: 32, port: 4000, url: 'http://localhost:4000/health',
    probe: 'http://localhost:4000/health', isOurs: (body) => body.includes('"status":"ok"'),
  },
  {
    name: 'tienda', dir: 'storefront-react', color: 36, port: 5173, url: 'http://localhost:5173',
    probe: 'http://localhost:5173/', isOurs: (body) => body.includes('Tienda Jamix Fashion'),
  },
  {
    name: 'admin', dir: 'admin-vue', color: 35, port: 5174, url: 'http://localhost:5174',
    probe: 'http://localhost:5174/', isOurs: (body) => body.includes('Jamix Admin'),
  },
];

// Which services take part in each command (leads-bff has no build; the admin has no tests yet).
const PARTICIPANTS = {
  install: SERVICES,
  dev: SERVICES,
  test: SERVICES.filter((s) => s.dir !== 'admin-vue'),
  build: SERVICES.filter((s) => s.dir !== 'leads-bff'),
};

const command = process.argv[2];
if (!PARTICIPANTS[command]) {
  console.error(`Uso: node scripts/run-all.mjs <${Object.keys(PARTICIPANTS).join('|')}>`);
  process.exit(1);
}

// A single command string: npm is npm.cmd on Windows (needs a shell), and passing an args array
// together with shell: true is deprecated in Node (DEP0190).
const npmCommand = (cmd) => (cmd === 'install' ? 'npm install --no-fund --no-audit' : `npm run ${cmd}`);
const label = (s) => `\x1b[${s.color}m[${s.name}]\x1b[0m`;

if (command !== 'dev') {
  // Sequential: readable output and a clear failing step.
  for (const s of PARTICIPANTS[command]) {
    console.log(`\n${label(s)} ${npmCommand(command)}`);
    const result = spawnSync(npmCommand(command), { cwd: `${root}/${s.dir}`, stdio: 'inherit', shell: true });
    if (result.status !== 0) {
      console.error(`\n${label(s)} falló (código ${result.status}).`);
      process.exit(result.status ?? 1);
    }
  }
  console.log('\nListo.');
  process.exit(0);
}

// ---------------------------------------------------------------- dev

/** True if something accepts TCP connections on the port (IPv4 or IPv6 loopback). */
function portInUse(port) {
  const tryHost = (host) =>
    new Promise((resolve) => {
      const socket = net.connect({ port, host });
      socket.setTimeout(1000);
      socket.once('connect', () => { socket.destroy(); resolve(true); });
      socket.once('timeout', () => { socket.destroy(); resolve(false); });
      socket.once('error', () => resolve(false));
    });
  return tryHost('127.0.0.1').then((v4) => v4 || tryHost('::1'));
}

async function answersAsOurs(s) {
  try {
    const res = await fetch(s.probe, { signal: AbortSignal.timeout(2000) });
    return res.ok && s.isOurs(await res.text());
  } catch {
    return false;
  }
}

/** Classifies every service as 'free', 'reuse' (already running) or 'busy' (port taken by something else). */
async function checkPorts() {
  return Promise.all(
    SERVICES.map(async (s) => {
      if (!(await portInUse(s.port))) return { s, state: 'free' };
      return { s, state: (await answersAsOurs(s)) ? 'reuse' : 'busy' };
    }),
  );
}

const children = [];
let shuttingDown = false;

function prefixLines(s, stream, target) {
  let buffer = '';
  stream.setEncoding('utf8');
  stream.on('data', (chunk) => {
    buffer += chunk;
    const lines = buffer.split(/\r?\n/);
    buffer = lines.pop();
    for (const line of lines) target.write(`${label(s)} ${line}\n`);
  });
  stream.on('end', () => buffer && target.write(`${label(s)} ${buffer}\n`));
}

function stopAll(exitCode) {
  if (shuttingDown) return;
  shuttingDown = true;
  // Only what this launcher started: services reused from another terminal keep running.
  for (const { child } of children) {
    if (child.exitCode !== null || !child.pid) continue;
    // With shell: true, child.kill() would leave java/node grandchildren alive on Windows.
    if (isWindows) spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
    else child.kill('SIGTERM');
  }
  process.exitCode = exitCode;
}

// --h2: the catalog uses the in-memory database instead of MySQL (no .env needed).
const useH2 = process.argv.includes('--h2');

function start(s) {
  const script = useH2 && s.dir === 'catalog-service' ? 'dev:h2' : 'dev';
  const child = spawn(npmCommand(script), { cwd: `${root}/${s.dir}`, shell: true });
  children.push({ s, child });
  prefixLines(s, child.stdout, process.stdout);
  prefixLines(s, child.stderr, process.stderr);
  child.on('exit', (code) => {
    if (shuttingDown) return;
    console.error(`${label(s)} terminó (código ${code}). Deteniendo los demás servicios...`);
    stopAll(code || 1);
  });
}

/** Resolves once the catalog API answers, so the front ends never load against a service that isn't up yet. */
async function waitForCatalog(timeoutMs = 180_000) {
  const catalog = SERVICES[0];
  const deadline = Date.now() + timeoutMs;
  while (!shuttingDown && Date.now() < deadline) {
    if (await answersAsOurs(catalog)) return true;
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  return false;
}

const ports = await checkPorts();

const busy = ports.filter((p) => p.state === 'busy');
if (busy.length > 0) {
  const findCmd = isWindows ? (port) => `netstat -ano | findstr :${port}` : (port) => `lsof -i :${port}`;
  console.error(
    [
      'No se puede arrancar: estos puertos están ocupados por otro programa (no por Jamix):',
      ...busy.map(({ s }) => `  ${label(s)} puerto ${s.port}  → para ver qué proceso es: ${findCmd(s.port)}`),
      'Cierra ese programa (o la terminal donde corre) y vuelve a ejecutar npm run dev.',
    ].join('\n'),
  );
  process.exit(1);
}

for (const { s, state } of ports) {
  if (state === 'reuse') console.log(`${label(s)} ya está corriendo en el puerto ${s.port} (otra terminal): lo reutilizo.`);
}

// The catalog needs MySQL credentials. Check before starting anything, instead of launching
// four services only to stop them when Spring Boot refuses to start.
const catalogWillStart = ports.some(({ s, state }) => s.dir === 'catalog-service' && state === 'free');
const hasDbConfig = existsSync(`${root}/catalog-service/.env`) || Boolean(process.env.DB_PASSWORD);
if (catalogWillStart && !useH2 && !hasDbConfig) {
  console.error(
    [
      `${label(SERVICES[0])} Falta la configuración de MySQL: no existe catalog-service/.env.`,
      '',
      '  Opción A: probar ya, sin MySQL (datos en memoria, se borran al cerrar):',
      '      npm run dev:h2',
      '',
      '  Opción B: usar tu MySQL (una sola vez; detalles en el README.md de la raíz):',
      '      1. Crea la base y los usuarios con catalog-service/db/setup-mysql.sql',
      '      2. copy catalog-service\\.env.example catalog-service\\.env   y completa las contraseñas',
      '      3. npm run dev',
    ].join('\n'),
  );
  process.exit(1);
}

process.on('SIGINT', () => stopAll(0));
process.on('SIGTERM', () => stopAll(0));

const toStart = new Set(ports.filter((p) => p.state === 'free').map((p) => p.s));
const [catalog, leads, ...frontEnds] = SERVICES;

console.log('Iniciando Jamix (Ctrl+C para detener todo). Esperando a Spring Boot...\n');
if (toStart.has(catalog)) start(catalog);
if (toStart.has(leads)) start(leads); // independent of the catalog

if (await waitForCatalog()) {
  for (const s of frontEnds) if (toStart.has(s)) start(s);
  console.log(['', 'Todo listo:', ...SERVICES.map((s) => `  ${label(s)} ${s.url}`), ''].join('\n'));
  if (children.length === 0) {
    console.log('Todos los servicios ya estaban corriendo en otras terminales; no inicié nada nuevo.');
  }
} else if (!shuttingDown) {
  console.error(`${label(catalog)} no respondió en 3 minutos. Revisa el log de arriba.`);
  stopAll(1);
}
