# Android Clean Architecture Template

Clean Architecture와 MVI 패턴을 사용한 Android 앱 템플릿입니다.

## 📦 Template as Git Submodule

이 템플릿은 Git submodule로 제공됩니다. 프로젝트에서 템플릿을 사용하려면:

### Submodule 초기화 및 업데이트
```bash
# 새로운 프로젝트를 클론한 경우
git submodule update --init --recursive

# 템플릿 최신 버전으로 업데이트
git submodule update --remote .claude
```

### 템플릿 직접 체크아웃
```bash
# .claude 디렉토리로 이동
cd .claude

# 특정 버전 또는 브랜치 체크아웃
git checkout main
git pull origin main
```

### Submodule URL
- Repository: https://github.com/hsikkk/android-template
- Path: `.claude/` (프로젝트 루트의 .claude 디렉토리)

## 📁 포함된 파일들

### 📖 문서
- `TEMPLATE_GUIDE.md` - 전체 아키텍처 설명과 사용 방법
- `TEMPLATE_USAGE.md` - 템플릿 사용법과 구체적인 예제
- `BUILD_ENVIRONMENT_GUIDE.md` - 빌드 환경 설정 및 최신 의존성 정보

### 🛠️ 템플릿 파일들

#### 프로젝트 설정
- `templates/settings.gradle.kts.template` - 프로젝트 모듈 설정
- `templates/app-build.gradle.kts.template` - 메인 앱 빌드 설정
- `templates/gradle.properties.template` - 프로젝트 설정 파일
- `templates/libs.versions.toml.template` - 의존성 버전 카탈로그
- `build-logic/` - 빌드 컨벤션 플러그인 (패키지명 교체 필요)

#### 피처 모듈 템플릿
- `templates/feature-ui-build.gradle.kts.template` - UI 모듈 빌드 설정
- `templates/feature-navigation-build.gradle.kts.template` - 내비게이션 모듈 빌드 설정

#### 코드 템플릿
- `templates/MainActivity.kt.template` - Hilt & SplashScreen 지원 MainActivity
- `templates/Application.kt.template` - Hilt Application 클래스
- `templates/AppScreen.kt.template` - 메인 앱 Compose 화면
- `templates/NavHost.kt.template` - Navigation Host 설정
- `templates/TabNavigation.kt.template` - Bottom Navigation Tab 구조
- `templates/SimpleFeatureScreen.kt.template` - 간단한 Feature 화면 템플릿
- `templates/SimpleFeatureViewModel.kt.template` - StateFlow 기반 간단한 ViewModel
- `templates/HiltModule.kt.template` - Hilt 의존성 주입 모듈
- `templates/FeatureScreen.kt.template` - Orbit-MVI Compose 화면 구성
- `templates/FeatureViewModel.kt.template` - Orbit-MVI ViewModel
- `templates/FeatureNavigation.kt.template` - 내비게이션 설정
- `templates/FeatureDirections.kt.template` - 내비게이션 경로 정의

#### MVI 패턴 Contract
- `templates/contract/FeatureState.kt.template` - UI 상태 정의
- `templates/contract/FeatureIntent.kt.template` - 사용자 의도 정의
- `templates/contract/FeatureSideEffect.kt.template` - 부수 효과 정의

## 🚀 시작하기

### 템플릿 사용 워크플로우

1. **Submodule 초기화**: `git submodule update --init` 명령으로 템플릿 가져오기
2. **환경 설정**: `BUILD_ENVIRONMENT_GUIDE.md`를 읽어 개발 환경을 설정하세요
3. **아키텍처 이해**: `TEMPLATE_GUIDE.md`를 읽어 전체 아키텍처를 이해하세요  
4. **템플릿 적용**: `TEMPLATE_USAGE.md`를 참고해 템플릿을 적용하세요
5. **커스터마이징**: 플레이스홀더를 실제 값으로 교체하고 검증 체크리스트를 확인하세요

### 템플릿 파일 적용 방법

템플릿 파일들은 `.claude/templates/` 디렉토리에 있습니다. 프로젝트에 적용하려면:

```bash
# 템플릿 파일을 프로젝트로 복사
cp .claude/templates/settings.gradle.kts.template settings.gradle.kts
cp .claude/templates/app-build.gradle.kts.template app/build.gradle.kts
# ... 기타 필요한 템플릿 파일들
```

## 🏗️ 아키텍처

- **Clean Architecture**: Domain, Data, Presentation 레이어 분리
- **Multi-module**: 기능별 모듈화
- **MVI Pattern**: Orbit-MVI를 사용한 단방향 데이터 플로우
- **Jetpack Compose**: 선언형 UI
- **Hilt**: 의존성 주입
- **Convention Plugins**: 표준화된 빌드 설정

### 🤖 AI 개발 지원
- **Claude Code Agents**: 개발 프로세스별 전문 AI 에이전트 (`agents/` 디렉토리)
  - Product visionary, Market researcher, Designer, Tech lead, Android developer, Localization expert
- **Orchestration System**: AI 에이전트 협업 시스템 (`orchestration/` 디렉토리)
- **Output Styles**: 맞춤형 출력 스타일 (`output-styles/` 디렉토리)

## ⚠️ 주요 개선사항 (2025년 1월)

### 최신 의존성 버전 (호환성 검증됨)
- **Android Gradle Plugin**: 8.7.2
- **Kotlin**: 2.0.21 (Compose Compiler 통합)
- **Compose BOM**: 2024.09.03
- **Hilt**: 2.52 (KSP 지원)
- **Navigation**: 2.8.1 (Type-safe Navigation)

### 완전한 설정 가이드
- **App Module 완전 설정**: MainActivity, Application 클래스, AndroidManifest.xml
- **Hilt & SplashScreen 통합**: 의존성 주입과 스플래시 화면 설정
- **Navigation 완전 구현**: Type-safe navigation, Tab navigation, Back stack 관리
- **Simple 템플릿 추가**: Orbit-MVI 없이 간단한 StateFlow 기반 템플릿 제공
- **일반적인 문제 해결**: 8가지 주요 이슈와 해결책
- **검증 체크리스트**: 7단계 단계별 검증 프로세스

### Clean Architecture 개선 (2025년 10월)
- **Domain 모듈 추가**: 순수 비즈니스 로직 레이어 (Model, Repository 인터페이스, UseCase)
- **DI 구조 개선**: 순환 참조 방지 패턴 적용 (AppContainer 개선)
- **Data 레이어 Adapter 패턴**: Repository 구현체를 Adapter로 구성
- **모듈 의존성 계층화**: 6단계 레이어로 명확한 의존성 방향 정립
- **Build-Logic 템플릿화**: Convention Plugin 전체 구조 템플릿 제공