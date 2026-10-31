import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

await import('./build-locales.mjs');

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

const pages = [
  { file: 'index.html', lang: 'ko', locale: 'ko_KR', route: '/' },
  { file: 'en.html', lang: 'en', locale: 'en_US', route: '/en' },
];
await rm(output, { recursive: true, force: true });
await mkdir(output, { recursive: true });
await cp(path.join(root, 'dist'), output, { recursive: true });
for (const page of pages) {
  let html = await readFile(path.join(root, 'dist', page.file), 'utf8');
  html = html.replace(/<link\b(?=[^>]*\brel="(?:canonical|alternate)")[^>]*>\s*/gi, '')
    .replace(/<meta\b(?=[^>]*\bproperty="og:(?:url|image|locale:alternate)")[^>]*>\s*/gi, '')
    .replace(/<meta\b(?=[^>]*\bname="robots")[^>]*>\s*/gi, '');
  const title = html.match(/<title>(.*?)<\/title>/)[1];
  const description = html.match(/<meta name="description" content="([^"]*)"/)[1];
  const image = `${origin ?? ''}/assets/social.png`;
  const metadata = [
    `<meta property="og:image" content="${escape(image)}">`,
    `<meta property="og:image:alt" content="un:short">`,
    `<meta name="twitter:title" content="${escape(title)}">`,
    `<meta name="twitter:description" content="${escape(description)}">`,
    `<meta name="twitter:image" content="${escape(image)}">`,
    `<meta name="robots" content="${production && origin ? 'index, follow, max-image-preview:large' : 'noindex, nofollow'}">`,
  ];
  if (origin) {
    metadata.push(`<link rel="canonical" href="${escape(origin + page.route)}">`,
      `<meta property="og:url" content="${escape(origin + page.route)}">`,
      ...pages.map(other => `<link rel="alternate" hreflang="${other.lang}" href="${escape(origin + other.route)}">`),
      `<link rel="alternate" hreflang="x-default" href="${escape(origin)}/">`,
      ...pages.filter(other => other.lang !== page.lang).map(other => `<meta property="og:locale:alternate" content="${other.locale}">`));
    const schema = {
      '@context': 'https://schema.org', '@type': 'SoftwareApplication',
      name: 'un:short', operatingSystem: 'Android 8.0+', applicationCategory: 'LifestyleApplication',
      url: origin + page.route, inLanguage: page.lang, description,
      image, installUrl: 'https://play.google.com/store/apps/details?id=com.muuu.unshort',
    };
    metadata.push(`<script type="application/ld+json">${JSON.stringify(schema).replaceAll('<', '\\u003c')}</script>`);
  }
  html = html.replace('</head>', `${metadata.join('\n')}\n</head>`);
  await writeFile(path.join(output, page.file), html);
}
if (production && origin) {
  await writeFile(path.join(output, 'robots.txt'), `User-agent: *\nAllow: /\nSitemap: ${origin}/sitemap.xml\n`);
  const urls = pages.map(page => `<url><loc>${escape(origin + page.route)}</loc></url>`).join('');
  await writeFile(path.join(output, 'sitemap.xml'), `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${urls}</urlset>\n`);
} else {
  await writeFile(path.join(output, 'robots.txt'), 'User-agent: *\nDisallow: /\n');
}
console.log(`Built .vercel-static (${production ? 'production' : 'preview/local'} metadata, ${pages.length} languages).`);
