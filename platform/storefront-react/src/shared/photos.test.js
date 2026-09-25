import assert from 'node:assert/strict';
import { test } from 'node:test';

import { photoSources } from './photos.js';

test('local photo: WebP variants from the storefront, original from the API as fallback', () => {
  assert.deepEqual(photoSources('/img/falda-bobo.jpeg', { apiBaseUrl: 'https://api.example.com' }), {
    src: 'https://api.example.com/img/falda-bobo.jpeg',
    srcSet: '/img/opt/falda-bobo-480.webp 480w, /img/opt/falda-bobo-960.webp 960w',
  });
});

test('behind the gateway the variants live under the base path', () => {
  assert.match(photoSources('/img/top-lazo.JPG', { assetBase: '/tienda' }).srcSet, /^\/tienda\/img\/opt\/top-lazo-480\.webp 480w/);
});

test('external or non-photo URLs keep only the original', () => {
  assert.deepEqual(photoSources('https://cdn.example.com/a.jpeg'), { src: 'https://cdn.example.com/a.jpeg', srcSet: undefined });
  assert.equal(photoSources('/img/logo.svg').srcSet, undefined);
});
