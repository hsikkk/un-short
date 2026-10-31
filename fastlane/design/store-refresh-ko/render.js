// Render actual Android captures into Korean Play Store artwork.
// Run from anywhere: node fastlane/design/store-refresh-ko/render.js
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');
const root = __dirname;
const output = path.resolve(root, '../../metadata/android/ko-KR/images');
const shots = [
  {file:'1_01-main', source:'main', bg:'#e9f0d8', ink:'#182016', kicker:'쇼츠 · 릴스 선택 차단', title:'필요한 앱은 그대로.<br>쇼츠는 잠깐 멈춤.', sub:'앱을 지우지 않고, 무심코 보는 순간을 차단해요.'},
  {file:'2_02-timer', source:'timer', bg:'#171c18', ink:'#f4f5ee', kicker:'폰 뒤집기 타이머', title:'폰을 뒤집으면,<br>스크롤도 쉬어갑니다.', sub:'잠깐 내려놓고, 계속 볼지 직접 선택하세요.'},
  {file:'3_03-tasks', source:'tasks', bg:'#efe9df', ink:'#26231e', kicker:'할 일 잠금 · BETA', title:'할 일을 먼저.<br>쇼츠는 그다음에.', sub:'오늘 할 일을 완료할 때까지 쇼츠를 잠가요.'},
  {file:'4_04-limits', source:'limits', bg:'#e2e9f1', ink:'#1d2938', kicker:'수면 모드 · 일일 사용 제한', title:'잠들기 전에도,<br>정한 시간만큼만.', sub:'취침 시간과 하루 시청 한도를 설정하세요.'}
];
function imageData(name) { return 'data:image/png;base64,'+fs.readFileSync(path.join(root,'sources',name+'.png')).toString('base64'); }
const base = `*{box-sizing:border-box}html,body{margin:0}body{font-family:'Apple SD Gothic Neo','Noto Sans CJK KR',sans-serif;-webkit-font-smoothing:antialiased} .art{width:1080px;height:1920px;position:relative;overflow:hidden;padding:50px 64px} .top{display:flex;justify-content:space-between;align-items:center;font-size:25px;font-weight:600;letter-spacing:-.5px}.brand{font-weight:800;font-size:29px}h1{font-size:76px;line-height:1.1;letter-spacing:-4px;margin:24px 0 18px;font-weight:800}p{font-size:29px;line-height:1.3;margin:0;letter-spacing:-.9px;opacity:.76}.screen{position:absolute;top:389px;left:190px;width:700px;height:1433px;overflow:hidden;border-radius:24px;box-shadow:0 24px 60px #00000022;border:1px solid #00000018;background:white}.screen img{width:700px;position:absolute;top:-41px;left:0}.index{position:absolute;bottom:32px;left:64px;font-size:20px;letter-spacing:3px;opacity:.5}.dots{position:absolute;bottom:39px;right:64px;display:flex;gap:8px}.dots i{width:7px;height:7px;border-radius:50%;background:currentColor;opacity:.2}.dots i.active{width:25px;border-radius:5px;opacity:.8}`;
(async()=>{
 fs.mkdirSync(path.join(output,'phoneScreenshots'),{recursive:true});
 const browser = await chromium.launch({headless:true});
 try {
  const page=await browser.newPage({viewport:{width:1080,height:1920},deviceScaleFactor:1});
  for(let i=0;i<shots.length;i++){
   const s=shots[i];
   const html=`<!doctype html><html lang="ko"><meta charset="utf-8"><title>${s.kicker}</title><style>${base}</style><main class="art" style="background:${s.bg};color:${s.ink}"><div class="top"><span>${s.kicker}</span><span class="brand">un:short</span></div><h1>${s.title}</h1><p>${s.sub}</p><div class="screen"><img src="${imageData(s.source)}" alt="un:short 실제 ${s.kicker} 화면"></div><span class="index">0${i+1} / 04</span><div class="dots">${shots.map((_,j)=>`<i class="${i===j?'active':''}"></i>`).join('')}</div></main></html>`;
   fs.writeFileSync(path.join(root,s.file+'.html'),html);
   await page.setContent(html); await page.evaluate(()=>document.fonts.ready);
   await page.locator('img').evaluate(img=>img.decode());
   const fits=await page.locator('h1').evaluate(e=>e.scrollWidth<=e.clientWidth);
   if(!fits) throw new Error('Heading overflow: '+s.file);
   await page.screenshot({path:path.join(output,'phoneScreenshots',s.file+'.png')});
  }
  const feature=`<!doctype html><html lang="ko"><meta charset="utf-8"><style>*{box-sizing:border-box}body{margin:0;font-family:'Apple SD Gothic Neo',sans-serif;background:#dce8c7;color:#182016}.feature{width:1024px;height:500px;position:relative;overflow:hidden;padding:74px 78px}.brand{font-size:24px;font-weight:800;margin-bottom:27px}h1{font-size:58px;line-height:1.13;letter-spacing:-3px;margin:0}p{font-size:23px;margin-top:22px;opacity:.7}.ring{position:absolute;width:278px;height:278px;border:2px solid #182016;border-radius:50%;right:65px;top:111px;display:flex;align-items:center;justify-content:center;gap:21px;transform:rotate(-8deg)}.bar{width:26px;height:100px;border-radius:8px;background:#182016}.orb{position:absolute;width:420px;height:420px;border:1px solid #18201618;border-radius:50%;right:-6px;top:40px}</style><main class="feature"><div class="orb"></div><div class="brand">un:short</div><h1>무심코 넘긴 시간,<br>나에게 돌려주세요.</h1><p>쇼츠와 릴스 앞에, 잠깐의 멈춤.</p><div class="ring"><span class="bar"></span><span class="bar"></span></div></main></html>`;
  fs.writeFileSync(path.join(root,'feature.html'),feature);
  await page.setViewportSize({width:1024,height:500}); await page.setContent(feature); await page.evaluate(()=>document.fonts.ready);
  await page.screenshot({path:path.join(output,'featureGraphic.png')});
  // Standalone contact sheet for review, never uploaded as a screenshot.
  const preview=`<!doctype html><html lang="ko"><meta charset="utf-8"><style>body{margin:0;background:#f7f7f4;padding:24px;font-family:sans-serif}.grid{display:flex;gap:16px}.grid img{width:270px;height:480px;box-shadow:0 4px 18px #0001}h2{font-size:18px;margin:0 0 18px}</style><h2>un:short · 한국어 스토어 스크린샷</h2><div class="grid">${shots.map(s=>`<img src="${pathToFileURL(path.join(output,'phoneScreenshots',s.file+'.png'))}">`).join('')}</div>`;
  fs.writeFileSync(path.join(root,'preview.html'),preview);
  await page.setViewportSize({width:1176,height:566}); await page.goto(pathToFileURL(path.join(root,'preview.html')).href);
  await page.locator('img').evaluateAll(imgs=>Promise.all(imgs.map(img=>img.decode())));
  await page.screenshot({path:path.join(root,'preview.png')});
  // Remove only known superseded Korean assets, after all new artwork succeeds.
  for (const name of ['3_03-question', '4_04-success', '1_01-problem', '2_02-solution', '3_03-feature', '4_04-result']) {
   fs.rmSync(path.join(output, 'phoneScreenshots', name + '.png'), {force:true});
  }
  console.log('Rendered 4 screenshots (1080×1920), feature graphic (1024×500), and preview.');
 } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exit(1)});
