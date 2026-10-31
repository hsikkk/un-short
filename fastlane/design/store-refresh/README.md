# Store listing refresh — 16 additional locales

The Korean listing is maintained in `../store-refresh-ko/`. This directory adds the other 16 supported languages: English, Japanese, simplified and traditional Chinese, Spanish, French, German, Brazilian Portuguese, Russian, Italian, Hindi, Arabic, Thai, Turkish, Vietnamese and Indonesian.

## Sources and generation

- `content.json`: localized titles, short/full descriptions, screenshot copy and feature-graphic copy. Indonesian is stored under the repository's `in` directory but uploaded to Play's canonical `id` locale (`playLocale`).
- `sources/<locale>/*.png`: real app UI captured from debug build 1.13.0 (39), 1080×2400. Demo tasks are localized examples, and usage statistics are not fabricated. Premium state is enabled for these captures.
- `capture.py`: capture automation for the isolated `emulator-5556`, using a temporary locale-switch helper granted CHANGE_CONFIGURATION. The helper changes Android's configuration; it does not replace app strings or alter screenshots. The helper source is in `capture-helper/` and is not shipped with the app. Build/sign it with a temporary development key, install it only on the capture emulator and grant the declared configuration permission. No signing keys or helper APKs are stored here.
- `render.js`: local Playwright renderer. Uses platform fonts, handles Arabic RTL and fits headings/subtitles before capture. Crops only system status/navigation bars from the actual UI.
- `previews/`: per-language contact sheets and four review sheets, never uploaded to Play. `index.html` provides a gallery of all copy and artwork. Some task-screen labels fall back to English in the current app resources; the actual UI is preserved, and the surrounding store copy and sample tasks are localized.

```sh
# Requires the existing fastlane/design Playwright dependency and Chromium.
node fastlane/design/store-refresh/render.js
# Render a subset while reviewing:
node fastlane/design/store-refresh/render.js en-US ar
```

Outputs are written to the existing `fastlane/metadata/android/<locale>` folders. Each locale has four 1080×1920 opaque RGB screenshots and one 1024×500 feature graphic. Known superseded screenshot filenames are removed after successful rendering.

Use these renderers after either older screenshot-generation script; those scripts still describe the earlier store listing. Run the Korean renderer last for Korean assets if needed.

## Upload and verification

`validate.py` checks text lengths, source locales, exact asset counts, PNG dimensions/color mode and file sizes. With `--stage <directory>`, it copies just these 16 listings into a staging directory, applying the `in` → `id` mapping.

Upload with Fastlane supply using `skip_upload_apk`, `skip_upload_aab`, and `skip_upload_changelogs`. Then read Play listings and image hashes again to compare all 48 text fields and 80 image files, including screenshot order. Verify that Korean metadata and release tracks remain unchanged. A successful API commit does not confirm public visibility or review completion.

Specifications: https://support.google.com/googleplay/android-developer/answer/9866151

## Final reconciliation (2026-10-31)

A fresh Play API read confirmed all 17 locales, including Korean: 51 text fields,
68 ordered screenshots and 17 feature graphics matched local files. No redundant upload
was performed. App binaries, release tracks and changelogs were not modified.
See `final-verification.json` for the comparison hashes. Public visibility/review completion
was not independently verified.
