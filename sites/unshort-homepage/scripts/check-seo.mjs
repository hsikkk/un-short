import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readFile, access } from 'node:fs/promises';
const build = new URL('build-vercel.mjs', import.meta.url);
const output = new URL('../.vercel-static/', import.meta.url);
const run = env => execFileSync(process.execPath, [build.pathname], {
  stdio: 'pipe',
  env: { ...process.env, SITE_URL: 'https://example.com', VERCEL_ENV: 'production', ...env },
});
run({});
for (const [file, lang, route] of [['index.html', 'ko', '/'], ['en.html', 'en', '/en']]) {
  const html = await readFile(new URL(file, output), 'utf8');
  assert.ok(html.includes(`<html lang="${lang}">`));
  assert.ok(html.includes(`<link rel="canonical" href="https://example.com${route}">`));
  for (const [code, url] of [['ko', '/'], ['en', '/en'], ['x-default', '/']]) {
    assert.ok(html.includes(`hreflang="${code}" href="https://example.com${url}"`));
  }
  assert.ok(!html.includes('chatgpt.site'));
  assert.ok(!html.includes('noindex'));
  const schema = JSON.parse(html.match(/<script type="application\/ld\+json">(.*?)<\/script>/s)[1]);
  assert.equal(schema.inLanguage, lang);
  assert.equal(schema.url, `https://example.com${route}`);
  assert.equal((html.match(/rel="canonical"/g) || []).length, 1);
  if (lang === 'en') assert.ok(!/[가-힣]/.test(html.replace('한국어', '')), 'Untranslated English page');
}
const sitemap = await readFile(new URL('sitemap.xml', output), 'utf8');
assert.ok(sitemap.includes('<loc>https://example.com/</loc>'));
assert.ok(sitemap.includes('<loc>https://example.com/en</loc>'));
run({ VERCEL_ENV: 'preview' });
for (const file of ['index.html', 'en.html']) {
  assert.ok((await readFile(new URL(file, output), 'utf8')).includes('noindex, nofollow'));
}
assert.equal(await readFile(new URL('robots.txt', output), 'utf8'), 'User-agent: *\nDisallow: /\n');
await assert.rejects(access(new URL('sitemap.xml', output)));
assert.throws(() => run({ SITE_URL: 'https://example.com/path' }), /SITE_URL must be/);
run({ SITE_URL: '', VERCEL_ENV: '', VERCEL: '', VERCEL_URL: '', VERCEL_PROJECT_PRODUCTION_URL: '' });
const local = await readFile(new URL('index.html', output), 'utf8');
assert.ok(!local.includes('rel="canonical"'));
assert.ok(local.includes('noindex'));
console.log('SEO checks passed: locale URLs, metadata, schema, sitemap, preview exclusion, origin validation.');
