const playwright = require('playwright');
const path = require('path');
const fs = require('fs');

async function captureImages() {
  const browser = await playwright.chromium.launch({
    channel: 'chrome',
    headless: true
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  // 출력 디렉토리 생성
  const outputDir = path.join(__dirname, '..', 'metadata', 'android');

  // Feature Graphics 캡처 (1024x500)
  console.log('📸 Capturing Feature Graphics...');

  for (const lang of ['ko-KR', 'en-US']) {
    const langCode = lang === 'ko-KR' ? 'ko' : 'en';
    const htmlPath = path.join(__dirname, 'feature-graphics', langCode, 'feature-graphic.html');
    const outputPath = path.join(outputDir, lang, 'images', 'featureGraphic.png');

    // 디렉토리 생성
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });

    await page.goto(`file://${htmlPath}`);
    await page.setViewportSize({ width: 1024, height: 500 });

    // 전체 페이지를 정확히 1024x500으로 캡처
    await page.screenshot({
      path: outputPath,
      clip: { x: 0, y: 0, width: 1024, height: 500 },
      animations: 'disabled'
    });

    console.log(`✅ ${lang} Feature Graphic saved to ${outputPath}`);
  }

  // Screenshots 캡처 (1080x2340 - 3x scale)
  console.log('\n📸 Capturing Screenshots...');

  for (const lang of ['ko-KR', 'en-US']) {
    const langCode = lang === 'ko-KR' ? 'ko' : 'en';
    const screenshotDir = path.join(outputDir, lang, 'images', 'phoneScreenshots');

    // 디렉토리 생성
    fs.mkdirSync(screenshotDir, { recursive: true });

    const screenshots = [
      '01-problem',
      '02-solution',
      '03-feature',
      '04-result'
    ];

    for (let i = 0; i < screenshots.length; i++) {
      const screenshotName = screenshots[i];
      const htmlPath = path.join(__dirname, 'screenshots', langCode, `${screenshotName}.html`);
      const outputPath = path.join(screenshotDir, `${i + 1}_${screenshotName}.png`);

      await page.goto(`file://${htmlPath}`);
      await page.setViewportSize({ width: 1080, height: 2340 });

      // 전체 페이지를 정확히 1080x2340으로 캡처
      await page.screenshot({
        path: outputPath,
        clip: { x: 0, y: 0, width: 1080, height: 2340 },
        animations: 'disabled'
      });

      console.log(`✅ ${lang} Screenshot ${i + 1} (${screenshotName}) saved`);
    }
  }

  await browser.close();
  console.log('\n🎉 All images captured successfully!');
}

captureImages().catch(console.error);
