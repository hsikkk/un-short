---
name: changelog
description: 새 버전의 changelog 항목을 ChangelogRegistry와 17개 언어 strings.xml에 추가
---

# Changelog Skill

앱 업데이트 시 사용자에게 보여줄 changelog 항목을 추가합니다.

## 트리거

- `/release` 워크플로우에서 changelog 미등록 시 자동 호출 (독립 실행 불가)

## 버전 결정 방식

- release가 이미 버전을 bump한 상태에서 호출되므로, `build.gradle.kts`의 값이 곧 새 버전

## 작업 순서

1. **대상 버전 확인**
   - `app/build.gradle.kts`에서 `versionCode`와 `versionName` 읽기 (이미 bump된 상태일 수 있음)

2. **기존 changelog 확인**
   - `app/src/main/java/com/muuu/unshort/changelog/ChangelogData.kt`의 `ChangelogRegistry`에서 현재 versionCode에 해당하는 항목이 있는지 확인
   - 이미 있으면 사용자에게 알리고 수정 여부 확인

3. **커밋 기록 기반 변경 항목 생성**
   - 마지막 Git 태그부터 현재까지의 커밋 기록을 조회:
     ```bash
     git log $(git describe --tags --abbrev=0)..HEAD --oneline
     ```
   - 커밋 내용을 분석하여 사용자 체감 변경사항을 추출
   - 추상적이고 간결한 항목으로 정리하여 사용자에게 제안
   - 사용자 확인 후 진행

4. **ChangelogRegistry 업데이트**
   - `ChangelogData.kt`의 `changelogs` 리스트 최상단에 새 항목 추가
   - string resource ID 형식: `R.string.changelog_v{versionName에서 .을 _로 변환}_item{N}`
   - 예: versionName "1.5.0" → `changelog_v1_5_0_item1`, `changelog_v1_5_0_item2`

5. **17개 언어 strings.xml에 번역 추가**
   - 각 파일의 `<!-- Changelog -->` 섹션에 추가
   - 이전 버전의 changelog 문자열은 **그대로 유지** (삭제하지 않음)
   - 대상 언어:
     - `values/` (영어, 기본)
     - `values-ko/` (한국어)
     - `values-ar/` (아랍어)
     - `values-de/` (독일어)
     - `values-es/` (스페인어)
     - `values-fr/` (프랑스어)
     - `values-hi/` (힌디어)
     - `values-in/` (인도네시아어)
     - `values-it/` (이탈리아어)
     - `values-ja/` (일본어)
     - `values-pt/` (포르투갈어)
     - `values-ru/` (러시아어)
     - `values-th/` (태국어)
     - `values-tr/` (터키어)
     - `values-vi/` (베트남어)
     - `values-zh-rCN/` (중국어 간체)
     - `values-zh-rTW/` (중국어 번체)

6. **빌드 검증**
   - `./gradlew assembleDebug`로 빌드 확인

7. **Git 커밋**
   - 커밋 메시지: `chore: v{versionName} changelog 추가`
   - changelog 관련 변경 파일만 staging하여 커밋

## changelog 항목 작성 원칙

- **사용자가 체감할 수 있는 변경사항만** 포함
- 내부 리팩토링, 번역 수정, 코드 정리 등은 제외
- 추상적이고 간결하게 표현 (예: "수면 모드 추가", "버그 수정 및 안정성 개선")
- 세부 구현 내용은 포함하지 않음 (예: ❌ "수면 모드 설정 UI를 BottomSheet로 변경")

## 검증 함수 (release skill에서 사용)

release skill에서 changelog 등록 여부를 확인할 때:
1. `ChangelogData.kt`에서 bump된 versionCode에 해당하는 항목이 있는지 확인
2. 없으면 이 skill을 실행하여 changelog를 먼저 추가한 후 release 계속 진행
3. 있으면 통과

## 사용 예시

```
User: /changelog
Assistant: 현재 버전: v1.5.0 (versionCode: 25)

마지막 태그(1.4.1) 이후 커밋:
- feat: 통계 리포트 차트 추가
- fix: 오버레이 깜빡임 버그 수정
- refactor: 내부 코드 정리

제안하는 changelog 항목:
1. 통계 리포트 차트 추가
2. 버그 수정 및 안정성 개선

이대로 진행할까요?

User: ㅇ
Assistant: [ChangelogRegistry 업데이트 + 17개 언어 번역 추가 + 빌드 검증 + 커밋]
```
