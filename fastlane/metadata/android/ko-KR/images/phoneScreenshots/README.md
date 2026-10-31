# 한국어 Play Store 스크린샷

2026-10-30 개편. 1080×1920 RGB PNG 4개를 아래 순서로 사용한다.

1. `1_01-main.png`: 필요한 앱은 그대로. 쇼츠는 잠깐 멈춤.
2. `2_02-timer.png`: 폰을 뒤집으면, 스크롤도 쉬어갑니다.
3. `3_03-tasks.png`: 할 일을 먼저. 쇼츠는 그다음에.
4. `4_04-limits.png`: 잠들기 전에도, 정한 시간만큼만.

원본 캡처, 생성 코드, 검토용 미리보기는 `fastlane/design/store-refresh-ko/`에 있다.

```sh
node fastlane/design/store-refresh-ko/render.js
```

전체 locale을 대상으로 하는 기존 `upload_screenshots` lane 대신 한국어 metadata만 별도 디렉터리에 복사하여 Fastlane supply로 업로드한다. 상세 절차는 위 생성 코드 디렉터리의 README를 참고한다.
