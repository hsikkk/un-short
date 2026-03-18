---
name: release
description: Android 앱의 버전 관리 및 프로덕션 배포를 위한 완전한 릴리스 워크플로우
---

# Release Skill

프로덕션 배포를 위한 완전한 릴리스 워크플로우를 실행합니다.

## 작업 순서

1. **버전 타입 선택**
   - 사용자에게 업데이트 내용에 따라 버전 타입을 물어봅니다:
     - `major`: 주요 기능 추가/변경 (예: 1.3.3 → 2.0.0)
     - `minor`: 새로운 기능 추가 (예: 1.3.3 → 1.4.0)
     - `patch`: 버그 수정/개선 (예: 1.3.3 → 1.3.4)
     - `code`: versionCode만 증가 (versionName 유지)

2. **현재 버전 확인**
   - `app/build.gradle.kts` 파일을 읽어서 현재 `versionCode`와 `versionName`을 확인합니다.

3. **버전 증가**
   - 선택된 타입에 따라 버전을 증가시킵니다:
     - `versionCode`는 항상 1 증가
     - `versionName`은 선택한 타입에 따라 증가 (code 타입은 제외)
   - `app/build.gradle.kts` 파일을 수정합니다.

4. **Changelog 검증 및 추가**
   - `app/src/main/java/com/muuu/unshort/changelog/ChangelogData.kt`의 `ChangelogRegistry`에서 bump된 versionCode에 해당하는 항목이 있는지 확인
   - 없으면 `/changelog` skill을 실행하여 changelog를 먼저 추가한 후 계속 진행
   - 있으면 통과

5. **Git 커밋 및 태그**
   - 변경사항을 커밋합니다:
     - major: `chore: bump version to X.X.X (versionCode) - major release`
     - minor: `chore: bump version to X.X.X (versionCode) - minor release`
     - patch: `chore: bump version to X.X.X (versionCode) - patch release`
     - code: `chore: bump version code to versionCode`
   - versionName이 변경된 경우 Git 태그를 생성합니다:
     - 태그명: `X.X.X` (예: `1.3.4`)
     - 태그 메시지: `Release vX.X.X (build versionCode)`

6. **Production 배포**
   - Fastlane `production` lane을 실행합니다:
     ```bash
     fastlane production
     ```
   - 이미 설정된 대로 100% 롤아웃, 메타데이터/스크린샷 업로드 없이 진행됩니다.

7. **완료 안내**
   - 릴리스 완료 후 사용자에게 다음 단계를 안내합니다:
     - `git push origin main`
     - `git push origin X.X.X` (태그가 생성된 경우)

## 중요 사항

- Git 상태가 clean한지 먼저 확인합니다 (커밋되지 않은 변경사항이 있으면 경고).
- Production 배포 전에 사용자에게 최종 확인을 받습니다.
- 모든 단계에서 명확한 피드백을 제공합니다.

## 사용 예시

```
User: /release
Assistant: 릴리스 워크플로우를 시작합니다. 업데이트 내용에 따라 버전 타입을 선택해주세요:
- major: 주요 기능 추가/변경
- minor: 새로운 기능 추가
- patch: 버그 수정/개선
- code: versionCode만 증가
```
