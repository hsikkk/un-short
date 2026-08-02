# un:short

un:short는 짧은 영상과 무한 피드 소비에 의도적인 마찰을 추가하는 Android 앱입니다. 접근성 서비스를 이용해 대상 화면을 감지하고, 타이머·폰 뒤집기·즉시 해제 한도로 충동적인 진입을 끊습니다.

## 현재 제공 기능

- Shorts/Reels 및 홈 피드 화면 감지·차단
- 사용자가 설정한 대기 시간과 폰 뒤집기 기반 해제 흐름
- 앱별 차단 설정, 일시 해제, 수면 모드, 일일 사용 제한
- 일별·시간대별 이용 통계와 리포트 알림
- 무료 사용자 즉시 해제 한도 및 리워드 광고 충전
- Google Play 구독과 Lifetime Premium 프로모션 코드

대상 앱의 실제 목록은 `AppBlockingRegistry`와 `FeedTargetRegistry`가 단일 기준입니다. 외부 앱 UI 변경 시 접근성 View ID 기반 감지가 영향을 받을 수 있습니다.

## 개발 환경

<!-- docs-sync:start -->
- Application ID: `com.muuu.unshort`
- 버전: `1.9.0` (`versionCode 33`)
- Android SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- JVM: Java 17
- 모듈: `:app`, `:affiliate`
- Shorts/Reels 대상: `YouTube Shorts`, `Instagram Reels`, `Facebook Reels`, `Naver Shorts`, `TikTok Shorts`
- 홈 피드 대상(Beta): `Instagram`, `YouTube`, `Threads`, `Facebook`
<!-- docs-sync:end -->

Android Studio 또는 JDK 17이 필요합니다. Firebase 설정과 사설 Maven 저장소 인증 정보는 저장소의 Gradle 설정을 따릅니다.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
python3 scripts/check_docs.py
```

디버그 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. 설치 후 앱의 안내에 따라 접근성 서비스와 다른 앱 위에 표시 권한을 허용해야 합니다. Android 13 이상에서는 알림 기능 사용 시 알림 권한도 필요합니다.

## 구조

```text
app/src/main/java/com/muuu/unshort/
├── service/blocking/   # Shorts/Reels 감지, 세션 상태, 오버레이 제어
├── feedblock/          # 홈 피드 감지와 차단
├── timer/              # 폰 뒤집기와 대기 타이머 화면
├── data/statistics/    # Room 기반 사용 통계
├── premium/            # 구독 및 Lifetime Premium
├── ad/                 # 광고 및 일일 즉시 해제 한도
├── receiver/           # 리셋, 알림, 수면 모드 수신기
└── ui/                 # 메인, 설정, 리포트, 다이얼로그
affiliate/              # 제휴 배너 Android library 모듈
documents/prd/          # 기능별 요구사항과 의사결정 기록
scripts/                # 문서 검사와 운영 도구
```

## 문서 안내

- [현재 제품 스펙](documents/SPEC.md): 현재 구현·출시 동작의 단일 진입점
- [문서 운영 가이드](documents/README.md): 소유권, 업데이트 트리거, 리뷰 주기
- [디자인 시스템](DESIGN_SYSTEM.md): 현재 UI 토큰과 구현 기준
- [PRD](documents/prd/): 기능 요구사항. 구현 완료 여부는 각 문서 상태를 확인
- [운영 스크립트](scripts/README.md): 프로모션 코드 도구

기능 개발 시 현재 동작은 제품 스펙에 반영하고, 결정 배경은 PRD에 남깁니다. 문서와 코드를 함께 바꾸는 규칙 및 자동 검사는 [문서 운영 가이드](documents/README.md)에 정의되어 있습니다.

## 주의사항

- 접근성 서비스는 민감한 권한이므로 수집 범위와 사용자 안내를 함께 검토합니다.
- 외부 앱의 화면 구조가 바뀌면 감지 레지스트리와 관련 문서를 같은 PR에서 갱신합니다.
- 릴리스 빌드와 배포에는 별도 서명·서비스 인증 정보가 필요합니다. 비밀값을 문서나 새 소스 파일에 추가하지 않습니다.

## 라이선스

저장소에 별도 라이선스 파일이 추가되기 전까지 라이선스가 명시적으로 부여된 것으로 간주하지 않습니다.
