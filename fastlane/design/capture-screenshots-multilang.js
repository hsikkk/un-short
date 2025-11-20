/**
 * Multi-language Play Store Screenshot Generator
 *
 * Generates 4 screenshots for each of 17 languages
 * Uses Playwright to capture high-quality screenshots
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

// Load translations
const translations = require('./translations.json');

const SCREENSHOT_WIDTH = 1080;
const SCREENSHOT_HEIGHT = 1920;
const BASE_DIR = path.join(__dirname, '../..');
const METADATA_DIR = path.join(BASE_DIR, 'fastlane/metadata/android');

// Language mapping (folder name -> translation key)
const LANGUAGES = [
  'ar', 'de-DE', 'en-US', 'es-ES', 'fr-FR',
  'hi-IN', 'in', 'it-IT', 'ja-JP', 'ko-KR',
  'pt-BR', 'ru-RU', 'th', 'tr-TR', 'vi',
  'zh-CN', 'zh-TW'
];

// Screenshot configurations
const SCREENSHOTS = [
  { id: 1, filename: '1_01-main' },
  { id: 2, filename: '2_02-timer' },
  { id: 3, filename: '3_03-question' },
  { id: 4, filename: '4_04-success' }
];

// Image paths
const IMAGES = {
  1: 'KakaoTalk_Photo_2025-11-20-11-12-19 004.jpeg',
  2: 'KakaoTalk_Photo_2025-11-20-11-12-18 002.jpeg',
  3: 'KakaoTalk_Photo_2025-11-20-11-12-19 003.jpeg',
  4: 'KakaoTalk_Photo_2025-11-20-11-12-18 001.jpeg'
};

/**
 * Generate HTML for a specific language and screenshot
 */
function generateHTML(lang, screenshotId) {
  const t = translations[lang] || translations['en-US'];
  const screenshot = `screenshot${screenshotId}`;
  const data = t[screenshot];

  // Read image and convert to base64
  const imagePath = path.join(__dirname, 'tmp', IMAGES[screenshotId]);
  const imageBuffer = fs.readFileSync(imagePath);
  const base64Image = imageBuffer.toString('base64');
  const imageDataUrl = `data:image/jpeg;base64,${base64Image}`;

  // Color for each screenshot
  const colors = {
    1: '#1A1A1A',
    2: '#7C3AED',
    3: '#DC2626',
    4: '#10B981'
  };

  return `<!DOCTYPE html>
<html lang="${lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Screenshot ${screenshotId}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Noto Sans', 'Noto Sans CJK KR', 'Noto Sans CJK JP', 'Noto Sans CJK SC', sans-serif;
            background: #FFFFFF;
        }

        .screenshot {
            width: ${SCREENSHOT_WIDTH}px;
            height: ${SCREENSHOT_HEIGHT}px;
            background: linear-gradient(180deg, #FAFAFA 0%, #FFFFFF 100%);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: space-between;
            padding: 100px 80px 120px;
        }

        .text-top {
            text-align: center;
            max-width: 900px;
        }

        .text-top h2 {
            font-size: 64px;
            font-weight: 800;
            line-height: 1.15;
            color: ${colors[screenshotId]};
            margin-bottom: 24px;
            letter-spacing: -0.02em;
        }

        .text-top p {
            font-size: 36px;
            font-weight: 400;
            color: #666;
            line-height: 1.4;
        }

        .phone-image {
            flex-shrink: 0;
            margin: 40px 0;
        }

        .phone-image img {
            display: block;
            width: 600px;
            height: auto;
            border-radius: 40px;
            box-shadow: 0 30px 80px rgba(0,0,0,0.2);
        }

        .brand-bottom {
            text-align: center;
        }

        .brand-bottom .logo {
            font-size: 48px;
            font-weight: 900;
            color: #1A1A1A;
            letter-spacing: 0.01em;
            margin-bottom: 8px;
        }

        .brand-bottom .tagline {
            font-size: 24px;
            color: #888;
            font-weight: 400;
        }
    </style>
</head>
<body>
    <div class="screenshot">
        <div class="text-top">
            <h2>${data.title}</h2>
            <p>${data.subtitle}</p>
        </div>
        <div class="phone-image">
            <img src="${imageDataUrl}" alt="Screenshot ${screenshotId}">
        </div>
        <div class="brand-bottom">
            <div class="logo">un:short</div>
            <div class="tagline">${data.tagline}</div>
        </div>
    </div>
</body>
</html>`;
}

/**
 * Capture screenshot using Playwright
 */
async function captureScreenshot(page, html, outputPath) {
  // Set content
  await page.setContent(html);

  // Wait for images to load
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);

  // Take screenshot
  await page.screenshot({
    path: outputPath,
    type: 'png',
    fullPage: false
  });
}

/**
 * Main function
 */
async function main() {
  console.log('🚀 Starting multi-language screenshot generation...\n');
  console.log(`📊 Languages: ${LANGUAGES.length}`);
  console.log(`📸 Screenshots per language: ${SCREENSHOTS.length}`);
  console.log(`📦 Total screenshots: ${LANGUAGES.length * SCREENSHOTS.length}\n`);

  // Launch browser
  const browser = await chromium.launch({
    headless: true
  });

  const context = await browser.newContext({
    viewport: {
      width: SCREENSHOT_WIDTH,
      height: SCREENSHOT_HEIGHT
    },
    deviceScaleFactor: 2 // High DPI
  });

  const page = await context.newPage();

  let totalCaptured = 0;
  let totalErrors = 0;

  // Process each language
  for (const lang of LANGUAGES) {
    console.log(`\n📂 Processing ${lang}...`);

    const outputDir = path.join(METADATA_DIR, lang, 'images/phoneScreenshots');

    // Create directory if it doesn't exist
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
      console.log(`   ✅ Created directory: ${outputDir}`);
    }

    // Check if translation exists
    if (!translations[lang]) {
      console.log(`   ⚠️  No translation found for ${lang}, using en-US`);
    }

    // Capture each screenshot
    for (const screenshot of SCREENSHOTS) {
      try {
        const html = generateHTML(lang, screenshot.id);
        const outputPath = path.join(outputDir, `${screenshot.filename}.png`);

        await captureScreenshot(page, html, outputPath);

        // Verify file was created
        if (fs.existsSync(outputPath)) {
          const stats = fs.statSync(outputPath);
          console.log(`   ✅ ${screenshot.filename}.png (${Math.round(stats.size / 1024)}KB)`);
          totalCaptured++;
        } else {
          console.log(`   ❌ Failed to create ${screenshot.filename}.png`);
          totalErrors++;
        }
      } catch (error) {
        console.error(`   ❌ Error capturing ${screenshot.filename}.png:`, error.message);
        totalErrors++;
      }
    }
  }

  await browser.close();

  console.log('\n' + '='.repeat(60));
  console.log('📊 Summary:');
  console.log(`   ✅ Successfully captured: ${totalCaptured}`);
  console.log(`   ❌ Errors: ${totalErrors}`);
  console.log(`   📁 Output: fastlane/metadata/android/*/images/phoneScreenshots/`);
  console.log('='.repeat(60) + '\n');

  if (totalErrors === 0) {
    console.log('🎉 All screenshots generated successfully!');
  } else {
    console.log('⚠️  Some screenshots failed. Please check the errors above.');
  }
}

// Run
main().catch(error => {
  console.error('❌ Fatal error:', error);
  process.exit(1);
});
