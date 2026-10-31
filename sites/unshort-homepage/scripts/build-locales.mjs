import { readFile, writeFile } from 'node:fs/promises';
const root = new URL('../', import.meta.url);
const translations = JSON.parse(await readFile(new URL('locales/en.json', root), 'utf8'));
const korean = await readFile(new URL('dist/index.html', root), 'utf8');
const english = korean.replace(/(?<=>)[^<>]+(?=<)|(?<=")[^"<>]+(?=")/g,
  text => translations[text] ?? text)
  .replace('<html lang="ko">', '<html lang="en">')
  .replace('content="ko_KR"', 'content="en_US"')
  .replace('hreflang="ko" aria-current="page"', 'hreflang="ko"')
  .replace('hreflang="en">', 'hreflang="en" aria-current="page">')
  .replaceAll('https://unshort-home-20261031.cloudyseal2048.chatgpt.site/"', 'https://unshort-home-20261031.cloudyseal2048.chatgpt.site/en"');
await writeFile(new URL('dist/en.html', root), english);
