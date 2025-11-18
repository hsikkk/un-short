# 스크린샷 추가 가이드

이 디렉토리에 앱의 스마트폰 스크린샷을 추가하세요.

## 요구사항

- **최소**: 2개
- **최대**: 8개
- **크기**: 320-3840px (세로 또는 가로)
- **형식**: PNG 또는 JPG

## 파일명 규칙

순서대로 표시하려면 숫자 접두사를 사용하세요:

```
1_main_screen.png
2_blocking_screen.png
3_settings.png
4_statistics.png
```

## 예시

1. 메인 화면
2. 차단 화면 (타이머 표시)
3. 설정 화면
4. 통계 화면
5. 권한 설정 화면

---

스크린샷 추가 후 다음 명령어로 업로드:

```bash
fastlane upload_screenshots
```
