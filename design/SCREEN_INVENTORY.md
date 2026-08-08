# 화면 인벤토리

- 마지막 대조: 2026-08-02
- 기준 디자인: `design/unshort.pen`

| Pen frame | Android 구현 | 상태 |
|---|---|---|
| Foundations / Components | `app/src/main/res/values/`, 공통 drawable/layout | Current |
| Main / Blocking On | `activity_main.xml`, `MainActivity.kt` | Current · Actual Render Verified |
| Permission Setup | `activity_permission_setup.xml`, `include_permission_cards.xml` | Needs Actual Capture |
| Shorts Block / Initial | `overlay_flip_phone.xml`, `ShortsBlockOverlayActivity.kt` | Needs Actual Capture |
| Settings | `activity_settings.xml`, `SettingsActivity.kt` | Current · Actual Render Verified |
| Settings / Full Scroll | `activity_settings.xml`, `SettingsActivity.kt`의 전체 스크롤 콘텐츠 | Derived from Verified Viewport |
| Report / Daily | `activity_report.xml`, `ReportActivity.kt` | Needs Actual Capture |
| Report / Full Scroll | `activity_report.xml`, `ReportActivity.kt`의 전체 스크롤 콘텐츠 | Needs Actual Capture |
| Premium Upgrade | `activity_premium_upgrade.xml`, `PremiumUpgradeActivity.kt` | Needs Actual Capture |
| Premium Upgrade / Full Scroll | `activity_premium_upgrade.xml`, `PremiumUpgradeActivity.kt`의 전체 스크롤 콘텐츠 | Needs Actual Capture |

`Current`는 출시 코드에 대응하는 기준 화면을 뜻합니다. 신규 탐색 화면은 이 표에 `Proposed`로 추가하고 구현 완료 후 `Current`로 전환합니다.

현재 기준 파일은 Foundations, 공통 컴포넌트, 주요 화면 6개와 전체 스크롤 companion 3개를 포함합니다. 기준 화면은 412×915로 관리하며 별도의 compact variant는 두지 않습니다. Main과 Settings만 실제 Android 캡처 대조를 통과했으며 나머지는 검증 완료 전까지 시각적 `Current`로 간주하지 않습니다.
