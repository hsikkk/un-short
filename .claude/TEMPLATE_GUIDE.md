# Android App Architecture Template

This is a template based on a clean architecture Android application using Jetpack Compose and modern Android development practices.

## 📁 Template Repository Structure

**Note**: This template is provided as a Git submodule. When you clone a project using this template, the `.claude` directory IS the template submodule itself.

### Using the Template as a Submodule

```bash
# Initialize and fetch the template submodule
git submodule update --init --recursive

# Update to latest template version
git submodule update --remote .claude
```

### Template Directory Structure

```
.claude/                              # 🎭 Template Root (Git Submodule)
├── orchestration/                   # AI Agent Orchestration System
│   ├── core-config.yaml            # Android-specific orchestration config
│   ├── agents/                      # Agent definitions
│   │   ├── elon-integrated.md       # Product visionary agent
│   │   ├── market-researcher-integrated.md # Market analysis agent
│   │   ├── tech-lead-integrated.md  # Architecture design agent
│   │   ├── designer-integrated.md   # Design system agent
│   │   └── android-developer-integrated.md # Implementation agent
│   ├── templates/                   # Document templates
│   │   ├── android-prd-template.md
│   │   ├── android-tech-spec-template.md
│   │   └── compose-design-spec-template.md
│   ├── workflows/                   # Workflow protocols
│   ├── checklists/                  # Quality gate checklists
│   └── tasks/                       # Automated tasks
├── agents/                          # Claude Code Sub-Agents  
│   ├── android-developer.md        # Android implementation specialist
│   ├── designer.md                  # Design system architect
│   ├── tech-lead.md                 # Architecture expert
│   ├── market-researcher.md         # Market analysis expert
│   ├── localization-expert.md      # L10N specialist
│   └── elon.md                      # Product visionary
├── output-styles/                   # Claude Code Output Styles
│   ├── technical-detail-focused.md # Clean Architecture 중심 기술 분석
│   ├── android-development-focused.md # Android 실무 개발 특화
│   └── compose-ui-focused.md        # Jetpack Compose UI 디자인 중심
├── templates/                       # 📝 Code Templates
│   ├── gradle/
│   │   └── libs.versions.toml.template  # Latest Android dependencies with Compose compiler
│   ├── gradle.properties.template   # Optimized build configuration
│   ├── app-build.gradle.kts.template
│   ├── feature-ui-build.gradle.kts.template
│   ├── FeatureScreen.kt.template    # Jetpack Compose screen
│   ├── FeatureViewModel.kt.template # MVI ViewModel pattern
│   └── contract/                    # MVI contract templates
├── build-logic/                     # Convention plugins (requires package update)
├── TEMPLATE_GUIDE.md                # This guide
├── TEMPLATE_USAGE.md                # Template usage instructions
├── BUILD_ENVIRONMENT_GUIDE.md       # Build environment setup
└── README.md                        # Quick start guide
```

### Your Project Structure (After Applying Template)

```
project/
├── app/                              # Main application module
├── core/                            # Core modules (shared across features)
│   ├── common/                      # Common utilities and extensions
│   ├── data/                        # Data layer implementation
│   ├── datasource/                  # Data source implementations
│   ├── designsystem/                # Design system components
│   ├── di/                          # Dependency injection modules
│   ├── domain/                      # Domain layer (entities, use cases)
│   ├── mediaplayer/                 # Media player functionality
│   ├── navigation/                  # Navigation utilities
│   └── viewmodel/                   # Base ViewModel classes
├── feature/                         # Feature modules
│   ├── {feature-name}/              # Feature-specific modules
│   │   ├── ui/                      # UI components and screens
│   │   └── navigation/              # Feature navigation
│   └── player/                      # Player feature example
└── .claude/                         # Template submodule (as shown above)
```

## 🏗️ Architecture Principles

### Clean Architecture Layers
1. **Domain Layer** (`core/domain`): Business logic, entities, and use cases
2. **Data Layer** (`core/data`, `core/datasource`): Data repositories and sources
3. **Presentation Layer** (`feature/*/ui`): UI components, ViewModels, and screens

### Modularization Strategy
- **App Module**: Main entry point, dependency wiring
- **Core Modules**: Shared functionality across features
- **Feature Modules**: Self-contained features with UI and navigation

## 🔧 Key Technologies & Patterns

### Build System
- **Gradle Version Catalogs**: Centralized dependency management
- **Convention Plugins**: Standardized build configuration
- **Multi-module setup**: Clear separation of concerns

### 🎭 AI-Powered Development Orchestration
- **Claude Code Agents**: Specialized AI agents for each development phase
- **Structured Workflows**: Template-based collaboration between agents
- **Quality Gates**: Automated validation at each development stage
- **Context Preservation**: Maintains project information across agent handoffs

### UI & Navigation
- **Jetpack Compose**: Modern declarative UI
- **Navigation Compose**: Type-safe navigation
- **MVI Pattern**: Unidirectional data flow with Orbit-MVI

### Dependency Injection
- **Hilt**: Dependency injection framework
- **Module-based DI**: Scoped dependencies per feature

### State Management
- **Orbit-MVI**: State management with Intent-State-SideEffect pattern
- **StateFlow**: Reactive state updates
- **Immutable Collections**: Consistent state representation

## 🚀 Getting Started

### 1. Setup Build Configuration
- Copy `build-logic/` directory to your project root
- Replace `{PACKAGE_NAME}` in convention plugins with your actual package name
- Copy `gradle.properties.template` to `gradle.properties` (customize SDK versions)
- Copy `libs.versions.toml.template` to `gradle/libs.versions.toml` (update dependency versions)
- Update `settings.gradle.kts` with your module structure

### 2. Create Core Modules
- `core/domain`: Define your business entities and use cases
- `core/data`: Implement repositories and data sources
- `core/designsystem`: Create reusable UI components
- `core/navigation`: Setup navigation utilities

### 3. Create Feature Modules
- `feature/{feature-name}/ui`: UI screens and ViewModels
- `feature/{feature-name}/navigation`: Navigation setup

### 4. Configure Dependencies
- Update `gradle/libs.versions.toml` with your dependencies
- Apply appropriate convention plugins to modules

## 📋 Module Templates

### Feature UI Module Template

```kotlin
// build.gradle.kts
plugins {
    id("feature.ui")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    // Add feature-specific dependencies
}
```

### Feature Navigation Module Template

```kotlin
// build.gradle.kts
plugins {
    id("feature.navigation")
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":feature:{feature-name}:ui"))
}
```

### Screen Implementation Pattern

```kotlin
@Composable
fun {Feature}Route(
    goBack: () -> Unit,
    // Add navigation callbacks
) {
    val viewModel: {Feature}ViewModel = hiltViewModel()
    val uiState by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            // Handle navigation side effects
        }
    }

    {Feature}Screen(
        uiState = uiState,
        onIntent = viewModel::onIntent
    )
}
```

### Navigation Setup Pattern

```kotlin
fun NavGraphBuilder.{feature}(
    navController: NavController,
) {
    composable({Feature}Directions.{screen}) {
        {Feature}Route(
            goBack = { navController.popBackStack() },
            // Add navigation callbacks
        )
    }
}
```

## 🛠️ Customization Checklist

### 📱 Android 프로젝트 설정
- [ ] Update package names in all modules
- [ ] Replace app name and identifiers in `app/build.gradle.kts`
- [ ] Customize theme in `core/designsystem`
- [ ] Update dependency versions in `gradle/libs.versions.toml`
- [ ] Configure Hilt modules for your specific use cases
- [ ] Add your domain-specific entities and use cases
- [ ] Create feature-specific UI components

### 🎭 AI Orchestration 설정
- [ ] `.claude/orchestration/core-config.yaml`에서 프로젝트 정보 업데이트
- [ ] `.claude/agents/` 폴더를 Claude Code 프로젝트에 복사
- [ ] Claude Code에서 `/agents` 명령어로 에이전트 활성화 확인
- [ ] `.claude/orchestration/templates/`의 문서 템플릿 프로젝트에 맞게 커스터마이징
- [ ] 품질 게이트 기준을 프로젝트 요구사항에 맞게 조정

### 🚀 권장 시작 워크플로우
1. **elon 에이전트로 제품 비전 수립**
   ```bash
   /agent elon
   "내 아이디어: [구체적인 앱 아이디어 설명]을 PRD로 작성해줘"
   ```

2. **market-researcher로 시장 검증**
   ```bash  
   /agent market-researcher
   "위 아이디어의 시장성을 분석해줘"
   ```

3. **tech-lead로 아키텍처 설계**
   ```bash
   /agent tech-lead  
   "Clean Architecture 기반으로 시스템을 설계해줘"
   ```

4. **designer로 UI/UX 설계**
   ```bash
   /agent designer
   "Material3 Design System을 구축해줘"
   ```

5. **android-developer로 구현**
   ```bash
   /agent android-developer
   "Jetpack Compose와 MVI 패턴으로 구현해줘"
   ```

## 🎨 Claude Code Output Styles

이 템플릿은 Android 개발에 특화된 Claude Code Output Styles를 제공합니다. 각 스타일은 특정 개발 상황에 최적화된 응답 패턴을 제공합니다.

### 사용 가능한 Output Styles

#### 🔧 technical-detail-focused
- **목적**: Clean Architecture와 기술적 세부사항 중심 분석
- **특징**: 
  - 아키텍처 계층 분석 및 검증
  - MVI 패턴 구현 가이드
  - 성능 최적화 및 품질 기준
  - Android 특화 워크플로우
- **사용 시기**: 복잡한 기능 구현, 아키텍처 설계, 성능 최적화

#### 📱 android-development-focused  
- **목적**: Android 실무 개발에 특화된 실용적 접근
- **특징**:
  - Domain-Data-Presentation 계층별 구현 가이드
  - Jetpack Compose + MVI 통합 패턴
  - Android 생태계 최적화
  - 실무 중심 코드 품질 기준
- **사용 시기**: 신규 기능 개발, 리팩토링, 아키텍처 개선

#### 🎨 compose-ui-focused
- **목적**: Jetpack Compose UI 개발과 디자인에 특화
- **특징**:
  - Material3 Design System 우선 적용
  - 상태 호이스팅과 컴포지션 최적화
  - 접근성과 반응형 디자인
  - 사용자 경험 중심 UI 설계
- **사용 시기**: UI/UX 개발, 디자인 시스템 구축, 컴포넌트 제작

### Output Styles 활용법

#### 1. Claude Code에서 설정
```bash
# Output Style 확인
/styles

# 특정 스타일 적용
/style technical-detail-focused    # 기술 분석 중심
/style android-development-focused # Android 개발 중심  
/style compose-ui-focused         # Compose UI 중심
```

#### 2. 프로젝트에서 복사 및 적용
```bash
# .claude/output-styles 폴더를 Claude Code 프로젝트에 복사
cp -r .claude/output-styles/ your-project/.claude/output-styles/

# Claude Code에서 로컬 스타일 인식
# (프로젝트의 .claude/output-styles/ 폴더를 자동 인식)
```

#### 3. 상황별 스타일 선택 가이드

**아키텍처 설계 및 분석**
```bash
/style technical-detail-focused
"Clean Architecture 기반으로 사용자 인증 시스템을 설계해줘"
```

**기능 구현 및 개발**  
```bash
/style android-development-focused
"프로필 편집 화면을 MVI 패턴으로 구현해줘"
```

**UI/UX 및 디자인**
```bash
/style compose-ui-focused  
"Material3 기반으로 온보딩 화면을 디자인해줘"
```

### 스타일별 특화 영역

| 개발 영역 | 권장 Output Style | 주요 특징 |
|----------|------------------|----------|
| **시스템 설계** | technical-detail-focused | 아키텍처 분석, 모듈 분리, 의존성 설계 |
| **비즈니스 로직** | android-development-focused | Use Case, Repository, Domain 모델 |
| **데이터 계층** | android-development-focused | Room, Retrofit, 데이터 매핑 |
| **UI 컴포넌트** | compose-ui-focused | Composable, 상태 관리, 애니메이션 |
| **디자인 시스템** | compose-ui-focused | Material3, 토큰, 접근성 |
| **성능 최적화** | technical-detail-focused | 프로파일링, 메모리 최적화 |
| **테스트** | android-development-focused | 단위 테스트, UI 테스트 |

### 통합 워크플로우 예시

복잡한 기능을 개발할 때 여러 스타일을 순차적으로 활용:

```bash
# 1. 아키텍처 설계
/style technical-detail-focused
"쇼핑 카트 기능의 Clean Architecture 설계안을 제시해줘"

# 2. 비즈니스 로직 구현  
/style android-development-focused
"위 설계를 바탕으로 Domain과 Data 계층을 구현해줘"

# 3. UI 구현
/style compose-ui-focused
"쇼핑 카트 화면을 Material3 디자인으로 구현해줘"
```

### 커스터마이징

각 Output Style은 프로젝트 요구사항에 맞게 수정 가능합니다:

```markdown
# .claude/output-styles/custom-style.md
---
name: Custom Project Style
description: 프로젝트 특화 커스텀 스타일
---

## 프로젝트별 특별 요구사항
- 특정 라이브러리 사용 강제
- 코딩 컨벤션 적용
- 특별한 아키텍처 패턴
```

## 📚 Best Practices

1. **Single Responsibility**: Each module has a clear, single purpose
2. **Dependency Direction**: Features depend on core, not other features
3. **Interface Segregation**: Use abstractions for cross-module communication
4. **Immutable State**: Use immutable data structures for state management
5. **Separation of Concerns**: Keep business logic in domain layer
6. **Testability**: Structure code for easy unit and integration testing

## 🎭 AI-Orchestrated Development Workflow

### Agent Chain Overview

이 템플릿은 Claude Code의 전문 에이전트들이 체계적으로 협업할 수 있도록 설계된 오케스트레이션 시스템을 포함합니다:

```
elon (제품 비전) → market-researcher (시장 검증) → 
tech-lead (Clean Architecture 설계) → designer (Material3 Design) → 
android-developer (Jetpack Compose 구현)
```

### 에이전트별 역할 및 산출물

#### 🚀 elon (Product Visionary)
- **역할**: 제품 비전 수립 및 PRD 작성
- **산출물**: 
  - Product Requirements Document (PRD)
  - 성공 지표 정의
  - 제품 로드맵
- **다음 단계**: market-researcher로 시장 검증 요청

#### 📊 market-researcher 
- **역할**: 모바일 시장 분석 및 경쟁 앱 조사
- **산출물**:
  - 시장 규모 분석 (TAM/SAM/SOM)
  - 경쟁 앱 분석
  - 사용자 검증 데이터
- **다음 단계**: tech-lead로 기술 설계 요청

#### 🏗️ tech-lead
- **역할**: Clean Architecture 기반 시스템 설계
- **산출물**:
  - 기술 아키텍처 명세서
  - 모듈 구조 정의 (app, core, feature)
  - API 설계 및 의존성 관리
- **다음 단계**: designer로 UI/UX 설계 요청

#### 🎨 designer
- **역할**: Material3 Design System 구축
- **산출물**:
  - Design System 명세서
  - Jetpack Compose 컴포넌트 라이브러리
  - UI/UX 가이드라인
- **다음 단계**: android-developer로 구현 요청

#### 📱 android-developer
- **역할**: Jetpack Compose + MVI 패턴 구현
- **산출물**:
  - 실제 소스 코드 (Kotlin/Compose)
  - 단위 테스트 및 UI 테스트
  - 빌드 및 배포 준비
- **완료**: 앱 출시 준비

### 품질 게이트 시스템

각 에이전트 전환 시점에서 자동 품질 검증:

#### PRD → Tech 전환 조건
- [ ] Android 제품 비전 명확 정의
- [ ] Play Store 요구사항 명시
- [ ] 모바일 성공 지표 수치화
- [ ] Android 권한 모델 고려

#### Tech → Design 전환 조건  
- [ ] Clean Architecture 문서화
- [ ] 모듈 구조 정의 완료
- [ ] Android API 명세 완료
- [ ] 의존성 버전 명시

#### Design → Dev 전환 조건
- [ ] 모든 Compose 화면 설계 완료
- [ ] Material3 Design System 완성
- [ ] 접근성 가이드라인 정의
- [ ] 애니메이션 명세 제공

#### Dev → Release 조건
- [ ] 모든 Android 테스트 통과
- [ ] Compose UI 테스트 완료
- [ ] 코드 커버리지 85% 이상
- [ ] Gradle 빌드 성공
- [ ] Play Store 메타데이터 준비

### 사용법

#### 1. 프로젝트 시작
```bash
# Claude Code에서 다음 명령어로 시작
"Android Clean Architecture 템플릿으로 새 프로젝트를 시작하고 싶어"
```

#### 2. 에이전트 체인 실행
```bash
# elon 에이전트로 제품 비전 수립
/agent elon
"피트니스 트래킹 앱 아이디어를 PRD로 작성해줘"

# 완료 후 자동으로 다음 에이전트 추천
# market-researcher → tech-lead → designer → android-developer
```

#### 3. 진행 상황 추적
각 에이전트는 `project-context.md`를 업데이트하여 프로젝트 상태를 추적하고, 다음 에이전트에게 필요한 컨텍스트를 전달합니다.

## 🔍 Example Usage

### 전통적인 개발 방식
```bash
# 개발자가 직접 모든 단계 수행
1. 아이디어 구상
2. 요구사항 정리
3. 아키텍처 설계  
4. UI 디자인
5. 구현
```

### AI-Orchestrated 개발 방식
```bash
# 에이전트들이 체계적으로 협업
1. elon: "혁신적인 피트니스 앱 PRD 작성"
2. market-researcher: "모바일 피트니스 시장 분석"
3. tech-lead: "Clean Architecture 기반 설계"
4. designer: "Material3 Design System 구축"
5. android-developer: "Jetpack Compose 구현"
```

### 기존 구현 참조
기존 `feature/browser` 구현을 참조하여 다음을 학습할 수 있습니다:
- 피처 모듈의 UI 및 navigation 설정
- Orbit MVI 패턴 구현
- 재사용 가능한 컴포넌트 생성
- 화면 간 내비게이션 처리