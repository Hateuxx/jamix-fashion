// Runs the Maven wrapper with the given arguments (mvnw.cmd on Windows, ./mvnw elsewhere),
// so `npm run dev` / `npm test` work here just like in the Node services. No global Maven needed.
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const cwd = fileURLToPath(new URL('..', import.meta.url));
const isWindows = process.platform === 'win32';
const args = process.argv.slice(2);

// Absolute, quoted path: some Windows setups (NoDefaultCurrentDirectoryInExePath) don't run
// programs from the current folder by bare name, and this project's path contains spaces.
// A .cmd file needs a shell; one command string (instead of args + shell) avoids Node's DEP0190.
const child = isWindows
  ? spawn(`"${cwd}mvnw.cmd" ${args.join(' ')}`, { cwd, stdio: 'inherit', shell: true })
  : spawn(`${cwd}mvnw`, args, { cwd, stdio: 'inherit' });

child.on('exit', (code, signal) => process.exit(signal ? 1 : (code ?? 0)));
