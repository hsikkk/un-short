// Localized Google Play assets from real Android captures. No network fonts.
const {chromium}=require('playwright');
const fs=require('fs');
const path=require('path');
const {pathToFileURL}=require('url');
const root=__dirname;
const content=require('./content.json');
const metadata=path.resolve(root,'../../metadata/android');
const sourceNames=['main','timer','tasks','limits'];
const files=['1_01-main','2_02-timer','3_03-tasks','4_04-limits'];
const backgrounds=['#e9f0d8','#171c18','#efe9df','#e2e9f1'];
const inks=['#182016','#f4f5ee','#26231e','#1d2938'];
const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const lines=s=>escape(s).replaceAll('\n','<br>');
const fonts={ar:"'Geeza Pro',Arial",'hi-IN':"'Kohinoor Devanagari','Devanagari Sangam MN'",th:"Thonburi,'Sukhumvit Set'",'ja-JP':"'Hiragino Sans'",'zh-CN':"'PingFang SC'",'zh-TW':"'PingFang TC'"};
function css(locale){return `*{box-sizing:border-box}html,body{margin:0}body{font-family:${fonts[locale]||"'Arial'"},sans-serif;-webkit-font-smoothing:antialiased}.art{width:1080px;height:1920px;position:relative;overflow:hidden;padding:50px 64px}.top{display:flex;justify-content:space-between;align-items:center;gap:24px;font-size:23px;font-weight:600}.kicker{max-width:750px}.brand{font-weight:800;font-size:29px;direction:ltr;flex-shrink:0}.copy{position:absolute;top:108px;left:64px;right:64px}h1{font-size:76px;line-height:1.18;letter-spacing:${['ar','hi-IN','th'].includes(locale)?'0':'-.025em'};margin:0 0 16px;font-weight:800}p{font-size:29px;line-height:1.3;margin:0;opacity:.78}.screen{position:absolute;top:389px;left:190px;width:700px;height:1433px;overflow:hidden;border-radius:24px;box-shadow:0 24px 60px #00000022;border:1px solid #00000018;background:white}.screen img{width:700px;position:absolute;top:-41px;left:0}.index{position:absolute;bottom:32px;left:64px;font-size:20px;letter-spacing:3px;opacity:.5}.dots{position:absolute;bottom:39px;right:64px;display:flex;gap:8px}.dots i{width:7px;height:7px;border-radius:50%;background:currentColor;opacity:.2}.dots i.active{width:25px;border-radius:5px;opacity:.8}`;}
async function fit(page,selector,maxHeight,minSize){
 return page.locator(selector).evaluate((el,{maxHeight,minSize})=>{
  for(let size=parseFloat(getComputedStyle(el).fontSize);size>=minSize;size--){
   el.style.fontSize=size+'px';
   if(el.scrollWidth<=el.clientWidth+1 && el.getBoundingClientRect().height<=maxHeight) return size;
  }
  throw new Error('Text overflow: '+el.textContent);
 },{maxHeight,minSize});
}
async function ready(page){await page.evaluate(()=>document.fonts.ready);await page.locator('img').evaluateAll(imgs=>Promise.all(imgs.map(i=>i.decode())));}
(async()=>{
 const requested=process.argv.slice(2);const locales=requested.length?requested:Object.keys(content);
 for(const locale of locales){if(!content[locale])throw new Error('Unknown locale '+locale);for(const name of sourceNames){if(!fs.existsSync(path.join(root,'sources',locale,name+'.png')))throw new Error('Missing capture '+locale+'/'+name);}}
 fs.mkdirSync(path.join(root,'previews'),{recursive:true});
 const browser=await chromium.launch({headless:true});const report=[];
 try{
  const page=await browser.newPage({viewport:{width:1080,height:1920},deviceScaleFactor:1});
  for(const locale of locales){
   const d=content[locale];const dir=locale==='ar'?'rtl':'ltr';const lang=locale==='in'?'id':locale;
   const out=path.join(metadata,locale);const images=path.join(out,'images');const shots=path.join(images,'phoneScreenshots');fs.mkdirSync(shots,{recursive:true});
   for(const [key,limit] of [['title',30],['short_description',80],['full_description',4000]]){
    if([...d[key]].length>limit)throw new Error('Metadata too long '+locale+'/'+key);
    fs.writeFileSync(path.join(out,key+'.txt'),d[key]+'\n');
   }
   await page.setViewportSize({width:1080,height:1920});
   for(let i=0;i<4;i++){
    const s=d.screenshots[i];const data='data:image/png;base64,'+fs.readFileSync(path.join(root,'sources',locale,sourceNames[i]+'.png')).toString('base64');
    await page.setContent(`<!doctype html><html lang="${lang}" dir="${dir}"><meta charset="utf-8"><title>${escape(s.kicker)}</title><style>${css(locale)}</style><main class="art" style="background:${backgrounds[i]};color:${inks[i]}"><div class="top"><span class="kicker">${escape(s.kicker)}</span><bdi class="brand">un:short</bdi></div><div class="copy"><h1>${lines(s.title)}</h1><p>${escape(s.subtitle)}</p></div><div class="screen"><img src="${data}" alt="${escape(s.kicker)}"></div><span class="index" dir="ltr">0${i+1} / 04</span><div class="dots">${files.map((_,j)=>`<i class="${i===j?'active':''}"></i>`).join('')}</div></main></html>`);
    await ready(page);
    const sourceDarkPixels=await page.locator('.screen img').evaluate(img=>{
     if(img.naturalWidth!==1080 || img.naturalHeight!==2400)throw new Error('Unexpected capture size');
     const canvas=document.createElement('canvas');canvas.width=1080;canvas.height=2400;
     const ctx=canvas.getContext('2d');ctx.drawImage(img,0,0);
     const pixels=ctx.getImageData(30,180,1020,2020).data;let dark=0;
     for(let j=0;j<pixels.length;j+=4)if(pixels[j]<160 && pixels[j+1]<160 && pixels[j+2]<160)dark++;
     return dark;
    });
    if(sourceDarkPixels<2000)throw new Error('Blank app capture '+locale+'/'+sourceNames[i]);
    const headingSize=await fit(page,'h1',182,48);const subtitleSize=await fit(page,'p',72,23);await fit(page,'.kicker',55,18);
    const bottom=await page.locator('.copy').evaluate(e=>e.getBoundingClientRect().bottom);if(bottom>365)throw new Error('Copy overlaps screen '+locale+' '+i);
    await page.screenshot({path:path.join(shots,files[i]+'.png')});report.push({locale,asset:files[i],headingSize,subtitleSize,sourceDarkPixels,copyBottom:bottom});
   }
   const side=dir==='rtl'?'right':'left';const symbol=dir==='rtl'?'left':'right';
   await page.setViewportSize({width:1024,height:500});
   await page.setContent(`<!doctype html><html lang="${lang}" dir="${dir}"><meta charset="utf-8"><style>${css(locale)}body{background:#dce8c7;color:#182016}.feature{width:1024px;height:500px;position:relative;overflow:hidden}.feature-copy{position:absolute;top:74px;${side}:64px;width:605px}.brand{font-size:24px;margin-bottom:26px;display:block}h1{font-size:58px;line-height:1.2;margin:0 0 22px}p{font-size:23px;line-height:1.3}.ring{position:absolute;width:246px;height:246px;border:2px solid #182016;border-radius:50%;${symbol}:55px;top:127px;display:flex;align-items:center;justify-content:center;gap:20px;transform:rotate(-8deg)}.bar{width:25px;height:90px;border-radius:8px;background:#182016}.orb{position:absolute;width:390px;height:390px;border:1px solid #18201618;border-radius:50%;${symbol}:-17px;top:55px}</style><main class="feature"><div class="orb"></div><div class="feature-copy"><bdi class="brand">un:short</bdi><h1>${lines(d.feature.title)}</h1><p>${escape(d.feature.subtitle)}</p></div><div class="ring"><span class="bar"></span><span class="bar"></span></div></main></html>`);
   await ready(page);const featureSize=await fit(page,'h1',160,36);await fit(page,'p',68,19);const fb=await page.locator('.feature-copy').evaluate(e=>e.getBoundingClientRect().bottom);if(fb>440)throw new Error('Feature overflow '+locale);
   await page.screenshot({path:path.join(images,'featureGraphic.png')});report.push({locale,asset:'featureGraphic',headingSize:featureSize});
   for(const old of ['3_03-question','4_04-success','1_01-problem','2_02-solution','3_03-feature','4_04-result'])fs.rmSync(path.join(shots,old+'.png'),{force:true});
   const thumbs=files.map(f=>`<img src="${pathToFileURL(path.join(shots,f+'.png'))}">`).join('');
   const preview=`<!doctype html><meta charset="utf-8"><style>body{margin:0;background:#f7f7f4;padding:24px;font-family:Arial}.grid{display:flex;gap:16px}img{width:270px;height:480px}h2{font-size:18px;margin:0 0 18px}</style><h2>un:short · ${locale}</h2><div class="grid">${thumbs}</div>`;
   const previewPath=path.join(root,'previews',locale+'.html');fs.writeFileSync(previewPath,preview);
   await page.setViewportSize({width:1176,height:566});await page.goto(pathToFileURL(previewPath).href);await ready(page);await page.screenshot({path:path.join(root,'previews',locale+'.png')});
   console.log(locale+': metadata, 4 screenshots, feature graphic, preview');
  }
  fs.writeFileSync(path.join(root,'layout-verification.json'),JSON.stringify(report,null,2)+'\n');
  // Four review sheets, four languages per sheet, every image included.
  const all=Object.keys(content);
  for(let group=0;group<4;group++){
   const set=all.slice(group*4,group*4+4);if(!set.every(l=>fs.existsSync(path.join(root,'previews',l+'.png'))))continue;
   const html=`<!doctype html><style>body{margin:0;background:#f7f7f4}img{width:1176px;height:566px;display:block}</style>`+set.map(l=>`<img src="${pathToFileURL(path.join(root,'previews',l+'.png'))}">`).join('');
   const p=path.join(root,'previews','sheet-'+(group+1)+'.html');fs.writeFileSync(p,html);await page.setViewportSize({width:1176,height:2264});await page.goto(pathToFileURL(p).href);await ready(page);await page.screenshot({path:path.join(root,'previews','sheet-'+(group+1)+'.png')});
  }
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
