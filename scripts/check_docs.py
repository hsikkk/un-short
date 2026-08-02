#!/usr/bin/env python3
"""Repository-local documentation consistency checks."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IGNORED_PARTS = {".git", ".gradle", ".idea", ".claude", ".omc", "build", "node_modules"}


def markdown_files() -> list[Path]:
    return sorted(
        path for path in ROOT.rglob("*.md")
        if not any(part in IGNORED_PARTS for part in path.parts)
    )


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def gradle_value(text: str, key: str) -> str:
    match = re.search(rf"^\s*{re.escape(key)}\s*=\s*[\"']?([^\"'\s]+)", text, re.MULTILINE)
    if not match:
        raise ValueError(f"cannot find {key}")
    return match.group(1)


def display_names(path: Path) -> list[str]:
    names = re.findall(r'displayName\s*=\s*"([^"]+)"', read(path))
    return list(dict.fromkeys(names))


def kotlin_int(text: str, name: str) -> str:
    match = re.search(rf"\b(?:const\s+val|val)\s+{re.escape(name)}\s*=\s*(\d+)", text)
    if not match:
        raise ValueError(f"cannot find {name}")
    return match.group(1)


def preference_default(text: str, key: str) -> str:
    match = re.search(rf"getInt\({re.escape(key)},\s*(\d+)\)", text)
    if not match:
        raise ValueError(f"cannot find default for {key}")
    return match.group(1)


def assigned_int(text: str, name: str) -> str:
    match = re.search(rf"\b{re.escape(name)}(?:\s*:\s*Int)?\s*=\s*(\d+)", text)
    if not match:
        raise ValueError(f"cannot find assigned value for {name}")
    return match.group(1)


def check_current_facts(errors: list[str]) -> None:
    app_gradle = read(ROOT / "app/build.gradle.kts")
    settings = read(ROOT / "settings.gradle.kts")
    readme = read(ROOT / "README.md")
    spec = read(ROOT / "documents/SPEC.md")
    preferences = read(ROOT / "app/src/main/java/com/muuu/unshort/prefs/PreferencesManager.kt")
    constants = read(ROOT / "app/src/main/java/com/muuu/unshort/config/AppConstants.kt")
    feed_preferences = read(ROOT / "app/src/main/java/com/muuu/unshort/feedblock/prefs/FeedBlockPreferences.kt")
    shorts_overlay = read(ROOT / "app/src/main/java/com/muuu/unshort/ui/activity/ShortsBlockOverlayActivity.kt")
    feed_overlay = read(ROOT / "app/src/main/java/com/muuu/unshort/feedblock/overlay/FeedBlockOverlayActivity.kt")
    shorts_names = display_names(
        ROOT / "app/src/main/java/com/muuu/unshort/service/blocking/AppBlockingRegistry.kt"
    )
    feed_names = display_names(
        ROOT / "app/src/main/java/com/muuu/unshort/feedblock/FeedTargetRegistry.kt"
    )
    modules = re.findall(r'include\("(:[^"\n]+)"\)', settings)
    readme_expected = [
        f"- Application ID: `{gradle_value(app_gradle, 'applicationId')}`",
        f"- 버전: `{gradle_value(app_gradle, 'versionName')}` (`versionCode {gradle_value(app_gradle, 'versionCode')}`)",
        f"- Android SDK: `minSdk {gradle_value(app_gradle, 'minSdk')}`, `targetSdk {gradle_value(app_gradle, 'targetSdk')}`, `compileSdk {gradle_value(app_gradle, 'compileSdk')}`",
        f"- JVM: Java {gradle_value(app_gradle, 'jvmTarget')}",
        f"- 모듈: {', '.join(f'`{module}`' for module in modules)}",
        "- Shorts/Reels 대상: " + ", ".join(f"`{name}`" for name in shorts_names),
        "- 홈 피드 대상(Beta): " + ", ".join(f"`{name}`" for name in feed_names),
    ]
    for line in readme_expected:
        if line not in readme:
            errors.append(f"README.md: current project fact is missing or stale: {line}")

    spec_expected = [
        f"- 대상 버전: `{gradle_value(app_gradle, 'versionName')}` (`versionCode {gradle_value(app_gradle, 'versionCode')}`)",
        f"- 플랫폼: Android `minSdk {gradle_value(app_gradle, 'minSdk')}`, `targetSdk {gradle_value(app_gradle, 'targetSdk')}`",
        "- Shorts/Reels 대상: " + ", ".join(f"`{name}`" for name in shorts_names),
        "- 홈 피드 대상(Beta): " + ", ".join(f"`{name}`" for name in feed_names),
        "- 홈 피드 Beta 기본 상태: `OFF`" if "getBoolean(KEY_BETA_ENABLED, false)" in feed_preferences else "__missing_beta_default__",
        f"기본 대기 시간은 `{preference_default(preferences, 'AppConstants.PREF_WAIT_TIME')}초`",
        f"| 쇼츠 보조 버튼 활성화 지연 | `{assigned_int(shorts_overlay, 'skipButtonCountdown')}초` | 즉시 |",
        f"| 피드 계속 보기 활성화 지연 | `{kotlin_int(feed_overlay, 'CONTINUE_DELAY_SECONDS')}초` | 즉시 |",
        f"- 무료 사용자 일일 기본 한도: `{kotlin_int(constants, 'DEFAULT_UNBLOCK_QUOTA_DAILY_LIMIT')}회`",
        f"- 한도 소진 후 리워드 광고 1회 완료: 기본 `{kotlin_int(constants, 'DEFAULT_UNBLOCK_QUOTA_AD_RECHARGE_AMOUNT')}회` 충전",
        f"- 일일 사용 제한: 기본 비활성, 기본 한도 `{preference_default(preferences, 'AppConstants.PREF_DAILY_LIMIT_MINUTES')}분`",
    ]
    for line in spec_expected:
        if line not in spec:
            errors.append(f"documents/SPEC.md: current product fact is missing or stale: {line}")


def check_links(errors: list[str]) -> None:
    link_pattern = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
    for document in markdown_files():
        for target in link_pattern.findall(read(document)):
            target = target.strip().split(maxsplit=1)[0].strip("<>")
            if not target or target.startswith(("#", "http://", "https://", "mailto:")):
                continue
            path_text = target.split("#", 1)[0]
            if path_text and not (document.parent / path_text).resolve().exists():
                errors.append(f"{document.relative_to(ROOT)}: broken link: {target}")


def check_portability(errors: list[str]) -> None:
    personal_path = re.compile(r"(?:`|\()(?:(?:/Users/[^/]+)|~)/")
    for document in markdown_files():
        for line_number, line in enumerate(read(document).splitlines(), start=1):
            if personal_path.search(line):
                errors.append(f"{document.relative_to(ROOT)}:{line_number}: personal absolute path is not portable")


def check_document_structure(errors: list[str]) -> None:
    required = [
        ROOT / "README.md",
        ROOT / "documents/README.md",
        ROOT / "documents/SPEC.md",
        ROOT / "DESIGN_SYSTEM.md",
    ]
    for path in required:
        if not path.exists():
            errors.append(f"required document is missing: {path.relative_to(ROOT)}")

    spec_link = "[현재 제품 스펙](../SPEC.md)"
    for prd in sorted((ROOT / "documents/prd").glob("*.md")):
        text = read(prd)
        if spec_link not in text:
            errors.append(f"{prd.relative_to(ROOT)}: current SPEC link is missing")
        if not re.search(r"(?:Status|상태).{0,20}(?:Draft|Proposed|Implemented|Superseded)", text):
            errors.append(f"{prd.relative_to(ROOT)}: recognized document status is missing")


def main() -> int:
    errors: list[str] = []
    check_current_facts(errors)
    check_links(errors)
    check_portability(errors)
    check_document_structure(errors)
    if errors:
        print("Documentation checks failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print(f"Documentation checks passed ({len(markdown_files())} Markdown files).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
