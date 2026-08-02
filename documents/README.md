# 문서 운영 가이드

- 상태: Active
- 소유자: 코드 변경을 작성·리뷰하는 팀 전체
- 마지막 검토: 2026-08-02
- 다음 정기 검토: 2026-11-02

## 단일 기준

문서는 코드를 복제하는 저장소가 아닙니다. [현재 제품 스펙](SPEC.md)은 사용자가 경험하는 현재 동작의 단일 진입점이고, 실행 가능한 세부 값은 아래에 지정된 코드가 최종 기준입니다.

| 정보 | 단일 기준 | 함께 확인할 문서 |
|---|---|---|
| 현재 제품 동작 | `documents/SPEC.md` | 구현 코드와 테스트 |
| 버전, SDK, JVM, 모듈 | `app/build.gradle.kts`, `affiliate/build.gradle.kts`, `settings.gradle.kts` | `SPEC.md`, `README.md` |
| Shorts/Reels 대상 앱 | `AppBlockingRegistry.kt` | `SPEC.md`, `README.md` |
| 피드 차단 대상 앱 | `FeedTargetRegistry.kt` | `SPEC.md`, `README.md`, 관련 PRD |
| 권한과 Android component | `AndroidManifest.xml` | `SPEC.md`, 관련 PRD |
| 색상, 폰트, 공통 style | `app/src/main/res/values/` | `DESIGN_SYSTEM.md` |
| 출시 변경 사항 | `ChangelogData.kt`와 string resource | README의 현재 버전 |
| 제품 정책과 배경 | `documents/prd/` | 구현 코드와 테스트 |

`.omc/plans/`와 `.claude/` 아래 파일은 도구가 만든 작업 기록 또는 로컬 설정으로, 유지 대상 제품 문서에 포함하지 않습니다. 유효한 결정이 있다면 PRD나 SPEC으로 옮깁니다.

## 변경 워크플로

1. 코드 변경 전에 위 표에서 영향을 받는 문서를 찾습니다.
2. 사용자 동작이 바뀌면 구현과 `SPEC.md`를 같은 PR에서 수정합니다. 결정 배경이 필요하면 PRD도 수정하고, 아직 구현되지 않은 내용은 `Draft` 또는 `Proposed`로 표시합니다.
3. `python3 scripts/check_docs.py`를 실행해 링크, 로컬 경로, SPEC/README의 코드 기반 정보를 검사합니다.
4. PR 템플릿의 문서 영향 항목을 작성합니다. 영향이 없다면 이유를 한 줄 남깁니다.
5. 리뷰어는 코드의 단일 기준과 문서 설명을 함께 비교합니다.

CI는 모든 push/PR에서 문서 검사를 수행합니다. 자동 검사가 제품 의미까지 판단할 수는 없으므로 PR 체크리스트와 분기별 정기 검토를 함께 사용합니다.

## 작성 규칙

- 파일 경로는 저장소 루트 기준 상대 경로를 사용하고 개인 홈 경로와 고정 line number를 쓰지 않습니다.
- PRD에는 `Status`, `Owner`, `Last Updated`를 둡니다. 구현된 기능은 `Implemented`, 폐기된 문서는 `Superseded`로 바꾸고 대체 문서를 링크합니다.
- `SPEC.md`에는 현재 구현된 동작만 씁니다. 제안, 회의 기록, 상세 구현 계획은 넣지 않습니다.
- 날짜가 필요한 정책 문서는 `YYYY-MM-DD` 형식을 사용합니다.
- 코드에서 쉽게 읽을 수 있는 상수와 목록을 여러 문서에 반복하지 않습니다.
- 실행 가능한 명령은 CI 또는 로컬에서 확인한 형태로만 기록합니다.
- 민감한 값, 실제 프로모션 코드, 개인 인증 정보는 문서와 예제에 넣지 않습니다.

## 정기 검토

분기마다 다음을 수행합니다.

- `Active`/`Implemented` 문서를 실제 코드와 대조
- 90일 이상 갱신되지 않은 `Draft`의 유지·폐기 결정
- 외부 앱 UI 변경으로 감지 대상과 설명이 달라졌는지 확인
- SPEC의 현재 동작과 README의 빌드 정보·명령 재검증
- 다음 검토일 갱신

정기 검토는 안전망이고, 변경 시점 업데이트가 기본 원칙입니다.
