# Play Store 스크린샷 템플릿

- 상태: Active
- 마지막 확인: 2026-08-02
- 언어: 17개
- 템플릿: 언어별 4개, 총 68개 HTML

`ko/`가 원본 템플릿이며 나머지 디렉터리는 번역본입니다. 각 언어에는 다음 화면이 있습니다.

1. `01-problem.html`: 문제 제시
2. `02-solution.html`: 타이머 기반 해결 방식
3. `03-feature.html`: 핵심 기능
4. `04-result.html`: 기대 효과

## 지원 디렉터리

`ar`, `de`, `en`, `es`, `fr`, `hi`, `in`, `it`, `ja`, `ko`, `pt`, `ru`, `th`, `tr`, `vi`, `zh-cn`, `zh-tw`

## 생성

Node.js 의존성을 설치한 뒤 `fastlane/design`에서 실행합니다.

```bash
npm ci
node capture-screenshots-multilang.js
```

번역이나 레이아웃을 바꾸면 17개 언어의 파일 수와 렌더링 결과를 함께 확인합니다. 생성된 이미지를 Play Store 메타데이터에 반영할 때는 locale 디렉터리 매핑도 확인합니다.
