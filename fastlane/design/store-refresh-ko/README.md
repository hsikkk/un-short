# 한국어 스토어 이미지

2026-10-30 개편. 실제 1.13.0 (39) 디버그 빌드를 Android 에뮬레이터에서 한국어로 촬영했다. 앱 화면을 재구성하지 않고 원본 캡처에 소개 문구와 배경을 덧붙인다. 상태 표시줄과 내비게이션 영역만 표시 범위에서 제외한다.

## 구성

1. 필요한 앱은 그대로. 쇼츠는 잠깐 멈춤. — 메인 화면
2. 폰을 뒤집으면, 스크롤도 쉬어갑니다. — 30초 대기 화면
3. 할 일을 먼저. 쇼츠는 그다음에. — 할 일 잠금 화면
4. 잠들기 전에도, 정한 시간만큼만. — 수면 모드 및 일일 제한 설정

할 일 목록은 촬영용 예시 데이터다. 사용 통계에 임의의 성과 수치를 넣지 않았다. 타이머 30초와 하루 제한 60분은 화면의 설정값이며 고정 정책을 의미하지 않는다. UI의 PRO 배지는 촬영에 사용한 프리미엄 상태를 나타낸다.

## 다시 생성

`fastlane/design`의 Playwright 의존성과 Chromium이 설치되어 있어야 한다. 한국어 글꼴은 macOS의 Apple SD Gothic Neo를 사용한다.

```sh
node fastlane/design/store-refresh-ko/render.js
```

- 원본: `sources/*.png` (1080×2400)
- 결과: `fastlane/metadata/android/ko-KR/images/phoneScreenshots/*.png` (1080×1920)
- 상단 소개 이미지: `fastlane/metadata/android/ko-KR/images/featureGraphic.png` (1024×500)
- 검토용: `preview.png`, `preview.html`

기존 `capture-images.js`와 `capture-screenshots-multilang.js`의 한국어 결과는 이전 버전이다. 전체 언어를 생성한 경우 위 명령을 마지막에 실행한다. 업로드 디렉터리에 이전 파일이 남아 있지 않은지 확인한다.

## 한국어만 업로드

한국어 metadata 디렉터리만 임시 루트에 복사한 뒤 Fastlane supply로 업로드한다. APK/AAB, 변경 내역 업로드는 생략한다. 업로드 후 Play API에서 한국어 문구, 이미지 수·순서·SHA-256을 다시 읽어 로컬 파일과 비교한다. API 커밋 성공과 실제 공개 페이지 반영 여부는 별도로 확인한다.

이미지 규격: https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko
