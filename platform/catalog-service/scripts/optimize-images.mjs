// Generates the WebP variants the storefront and the SSR page serve instead of the original JPEGs:
//   static/img/<name>.jpeg  ->  static/img/opt/<name>-480.webp and <name>-960.webp
// Run after adding or replacing a photo:  npm run images   (only changed photos are processed).
// The variants are committed, so builds and deploys never depend on sharp.
import { mkdirSync, readdirSync, statSync, unlinkSync } from 'node:fs';
import { join, parse } from 'node:path';
import { fileURLToPath } from 'node:url';

import sharp from 'sharp';

// Keep in sync with OPTIMIZED_WIDTHS in storefront-react/src/shared/photos.js and PhotoSources.java.
const WIDTHS = [480, 960];
const QUALITY = 72;

const imgDir = fileURLToPath(new URL('../src/main/resources/static/img/', import.meta.url));
const outDir = join(imgDir, 'opt');
mkdirSync(outDir, { recursive: true });

const sources = readdirSync(imgDir).filter((f) => /\.(jpe?g|png)$/i.test(f));
const expected = new Set();
let written = 0;

for (const file of sources) {
  const { name } = parse(file);
  const sourceTime = statSync(join(imgDir, file)).mtimeMs;
  for (const width of WIDTHS) {
    const target = `${name}-${width}.webp`;
    expected.add(target);
    const targetPath = join(outDir, target);
    let upToDate = false;
    try {
      upToDate = statSync(targetPath).mtimeMs >= sourceTime;
    } catch {
      // Not generated yet.
    }
    if (upToDate) continue;
    // withoutEnlargement: a 563 px photo stays 563 px in the "960" file (no blurry upscaling).
    await sharp(join(imgDir, file))
      .rotate()
      .resize({ width, withoutEnlargement: true })
      .webp({ quality: QUALITY })
      .toFile(targetPath);
    written++;
  }
}

// Variants of photos that no longer exist would otherwise ship forever.
let removed = 0;
for (const file of readdirSync(outDir)) {
  if (!expected.has(file)) {
    unlinkSync(join(outDir, file));
    removed++;
  }
}

const bytes = (dir, files) => files.reduce((sum, f) => sum + statSync(join(dir, f)).size, 0);
const kb = (n) => `${Math.round(n / 1024)} KB`;
const variants = readdirSync(outDir);
console.log(`${sources.length} fotos: ${written} variantes nuevas, ${removed} eliminadas.`);
console.log(`Originales: ${kb(bytes(imgDir, sources))}. WebP ${WIDTHS.join('/')} px: ${kb(bytes(outDir, variants.filter((f) => f.endsWith(`-${WIDTHS[0]}.webp`))))} / ${kb(bytes(outDir, variants.filter((f) => f.endsWith(`-${WIDTHS[1]}.webp`))))}.`);
