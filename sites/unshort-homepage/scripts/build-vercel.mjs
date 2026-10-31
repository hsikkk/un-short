import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const output = path.join(root, '.vercel-static');
const rawUrl = process.env.SITE_URL?.trim()
  || process.env.VERCEL_PROJECT_PRODUCTION_URL?.trim()
  || process.env.VERCEL_URL?.trim();
let origin;
if (rawUrl) {
  const url = new URL(rawUrl.includes('://') ? rawUrl : `https://${rawUrl}`);
  if (url.protocol !== 'https:' || url.username || url.password
      || url.pathname !== '/' || url.search || url.hash) {
    throw new Error('SITE_URL must be an HTTPS origin without credentials, path, query, or fragment.');
  }
  origin = url.origin;
} else if (process.env.VERCEL === '1') {
  throw new Error('Set SITE_URL or enable Vercel system environment variables for deployment metadata.');
}
const production = process.env.VERCEL_ENV === 'production';
const escape = value => value.replaceAll('&', '&amp;').replaceAll('"', '&quot;')
  .replaceAll('<', '&lt;').replaceAll('>', '&gt;');

let html = await readFile(path.join(root, 'dist/index.html'), 'utf8');
// Keep the Sites source untouched; only the Vercel output uses its deployment origin.
html = html.replace(/<link\b(?=[^>]*\brel="canonical")[^>]*>\s*/gi, '')
  .replace(/<meta\b(?=[^>]*\bproperty="og:(?:url|image)")[^>]*>\s*/gi, '')
  .replace(/<meta\b(?=[^>]*\bname="robots")[^>]*>\s*/gi, '');
const metadata = origin
  ? `<link rel="canonical" href="${escape(origin)}/">\n<meta property="og:url" content="${escape(origin)}/">\n<meta property="og:image" content="${escape(origin)}/assets/social.png">\n`
  : '<meta property="og:image" content="/assets/social.png">\n';
html = html.replace('</head>', `${metadata}${production ? '' : '<meta name="robots" content="noindex, nofollow">\n'}</head>`);
await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });
await cp(path.join(root, 'dist'), output, { recursive: true });
await writeFile(path.join(output, 'index.html'), html);
if (production && origin) {
  await writeFile(path.join(output, 'robots.txt'), `User-agent: *\nAllow: /\nSitemap: ${origin}/sitemap.xml\n`);
  await writeFile(path.join(output, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"><url><loc>${escape(origin)}/</loc></url></urlset>\n`);
} else {
  await writeFile(path.join(output, 'robots.txt'), 'User-agent: *\nDisallow: /\n');
}
console.log(`Built .vercel-static (${production ? 'production' : 'preview/local'} metadata).`);
