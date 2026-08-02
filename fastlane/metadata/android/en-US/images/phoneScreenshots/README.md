# English Play Store screenshots

This directory contains the ordered phone screenshots uploaded for the `en-US` listing.

- Current set: four PNG files (`1_` through `4_`)
- Keep numeric prefixes to preserve Play Store order.
- Replace the complete set when the product UI or store narrative changes.
- Verify screenshots contain no personal data, debug UI, or obsolete product behavior.

From the repository root, upload screenshots with:

```bash
bundle exec fastlane android upload_screenshots
```

The lane uploads every locale under `fastlane/metadata/android`; review all changed locale assets before running it.
