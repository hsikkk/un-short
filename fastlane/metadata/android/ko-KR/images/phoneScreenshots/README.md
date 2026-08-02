# 한국어 Play Store 스크린샷

이 디렉터리는 `ko-KR` 스토어 등록정보에 업로드할 휴대전화 스크린샷을 순서대로 보관합니다.

- 현재 구성: `1_`부터 `4_`까지 PNG 4개
- Play Store 표시 순서를 위해 숫자 접두사를 유지합니다.
- 제품 UI나 스토어 설명 흐름이 바뀌면 일부가 아니라 전체 세트를 함께 검토합니다.
- 개인정보, 디버그 UI, 더 이상 존재하지 않는 제품 동작이 포함되지 않았는지 확인합니다.

저장소 루트에서 다음 명령으로 업로드합니다.

```bash
bundle exec fastlane android upload_screenshots
```

이 lane은 `fastlane/metadata/android` 아래 모든 locale을 대상으로 하므로 실행 전에 변경된 전체 언어 이미지를 확인합니다.
