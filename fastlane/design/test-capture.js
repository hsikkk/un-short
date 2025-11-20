/**
 * Test screenshot capture with Playwright
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

async function test() {
  console.log('🧪 Testing Playwright screenshot capture...\n');

  try {
    const browser = await chromium.launch({
      headless: true
    });

    console.log('✅ Browser launched');

    const context = await browser.newContext({
      viewport: {
        width: 1080,
        height: 1920
      },
      deviceScaleFactor: 2
    });

    console.log('✅ Context created');

    const page = await context.newPage();
    console.log('✅ Page created');

    // Simple HTML
    const html = `<!DOCTYPE html>
<html>
<head>
    <style>
        body {
            margin: 0;
            width: 1080px;
            height: 1920px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: system-ui;
        }
        h1 {
            color: white;
            font-size: 72px;
            text-align: center;
        }
    </style>
</head>
<body>
    <h1>Test Screenshot<br>1080 x 1920</h1>
</body>
</html>`;

    await page.setContent(html);
    console.log('✅ Content set');

    await page.waitForTimeout(500);

    const outputPath = path.join(__dirname, 'test-output.png');
    await page.screenshot({
      path: outputPath,
      type: 'png',
      fullPage: false
    });

    console.log(`✅ Screenshot saved: ${outputPath}`);

    // Check file
    if (fs.existsSync(outputPath)) {
      const stats = fs.statSync(outputPath);
      console.log(`✅ File size: ${Math.round(stats.size / 1024)}KB`);
    }

    await browser.close();
    console.log('✅ Browser closed');

    console.log('\n🎉 Test successful!');
  } catch (error) {
    console.error('❌ Error:', error);
    process.exit(1);
  }
}

test();
