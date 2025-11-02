# Template Usage Guide

## 📦 Template as Git Submodule

This template is distributed as a Git submodule located at `.claude/` in your project. The submodule contains all
template files, AI agents, and documentation.

### Submodule Setup

```bash
# Clone a project with this template
git clone --recursive [your-project-url]

# Or if you already cloned without --recursive
git submodule update --init --recursive

# Update template to latest version
cd .claude
git checkout main
git pull origin main
cd ..
git add .claude
git commit -m "Update template to latest version"
```

### Template Repository

- **URL**: https://github.com/hsikkk/android-template
- **Local Path**: `.claude/`
- **Branch**: main

## 🚀 Quick Start

To use these templates for creating a new Android app or adding features to an existing app, follow these steps:

### 1. Global Replacements

Replace these placeholders throughout all template files:

**⚠️ Important: Don't forget to update the build-logic convention plugins package names!**

| Placeholder             | Example             | Description                         |
|-------------------------|---------------------|-------------------------------------|
| `{PROJECT_NAME}`        | `my-awesome-app`    | Project name in settings.gradle.kts |
| `{PACKAGE_NAME}`        | `com.company.myapp` | Base package name                   |
| `{APP_NAME}`            | `myapp`             | App module name                     |
| `{APP_NAME_PASCAL}`     | `MyApp`             | App name in PascalCase for theme    |
| `{FEATURE_NAME}`        | `profile`           | Feature name in lowercase           |
| `{FEATURE_NAME_PASCAL}` | `Profile`           | Feature name in PascalCase          |
| `{FEATURE_TITLE}`       | `프로필`               | Human-readable feature title        |

### 2. Creating a New Feature

#### Step 1: Create Module Structure

```
feature/
└── {FEATURE_NAME}/
    ├── ui/
    │   ├── build.gradle.kts (use feature-ui-build.gradle.kts.template)
    │   └── src/main/java/{PACKAGE_PATH}/{FEATURE_NAME}/ui/
    │       ├── {FEATURE_NAME_PASCAL}Screen.kt
    │       ├── {FEATURE_NAME_PASCAL}ViewModel.kt
    │       └── contract/
    │           ├── {FEATURE_NAME_PASCAL}State.kt
    │           ├── {FEATURE_NAME_PASCAL}Intent.kt
    │           └── {FEATURE_NAME_PASCAL}SideEffect.kt
    └── navigation/
        ├── build.gradle.kts (use feature-navigation-build.gradle.kts.template)
        └── src/main/java/{PACKAGE_PATH}/{FEATURE_NAME}/navigation/
            ├── {FEATURE_NAME_PASCAL}Navigation.kt
            └── {FEATURE_NAME_PASCAL}Directions.kt
```

#### Step 2: Update settings.gradle.kts

Add these lines:

```kotlin
include(":feature:{FEATURE_NAME}:ui")
include(":feature:{FEATURE_NAME}:navigation")
```

#### Step 3: Add Feature to App

In `app/build.gradle.kts`, add:

```kotlin
implementation(project(":feature:{FEATURE_NAME}:navigation"))
```

### 3. Template Files Usage

All template files are located in `.claude/templates/`. Copy them to your project and replace placeholders:

```bash
# Example: Copy settings.gradle template
cp .claude/templates/settings.gradle.kts.template settings.gradle.kts

# Example: Copy app build template
cp .claude/templates/app-build.gradle.kts.template app/build.gradle.kts
```

#### Core Build Files

- `.claude/templates/settings.gradle.kts.template` → `settings.gradle.kts`
- `.claude/templates/app-build.gradle.kts.template` → `app/build.gradle.kts`

#### Feature UI Module

- `.claude/templates/feature-ui-build.gradle.kts.template` → `feature/{FEATURE_NAME}/ui/build.gradle.kts`
- `.claude/templates/FeatureScreen.kt.template` →
  `feature/{FEATURE_NAME}/ui/src/main/java/.../ui/{FEATURE_NAME_PASCAL}Screen.kt`
- `.claude/templates/FeatureViewModel.kt.template` →
  `feature/{FEATURE_NAME}/ui/src/main/java/.../ui/{FEATURE_NAME_PASCAL}ViewModel.kt`

#### Feature Navigation Module

- `.claude/templates/feature-navigation-build.gradle.kts.template` →`feature/{FEATURE_NAME}/navigation/build.gradle.kts`
- `.claude/templates/FeatureNavigation.kt.template` →
  `feature/{FEATURE_NAME}/navigation/src/main/java/.../navigation/{FEATURE_NAME_PASCAL}Navigation.kt`
- `.claude/templates/FeatureDirections.kt.template` →
  `feature/{FEATURE_NAME}/navigation/src/main/java/.../navigation/{FEATURE_NAME_PASCAL}Directions.kt`

#### Contract Files (MVI Pattern)

- `.claude/templates/contract/FeatureState.kt.template` → `.../ui/contract/{FEATURE_NAME_PASCAL}State.kt`
- `.claude/templates/contract/FeatureIntent.kt.template` → `.../ui/contract/{FEATURE_NAME_PASCAL}Intent.kt`
- `.claude/templates/contract/FeatureSideEffect.kt.template` → `.../ui/contract/{FEATURE_NAME_PASCAL}SideEffect.kt`

### 4. Example: Creating a "Profile" Feature

1. **Replace placeholders:**
    - `{FEATURE_NAME}` → `profile`
    - `{FEATURE_NAME_PASCAL}` → `Profile`
    - `{FEATURE_TITLE}` → `프로필`
    - `{PACKAGE_NAME}` → `com.mycompany.myapp`

2. **Create directory structure:**
   ```
   feature/profile/ui/
   feature/profile/navigation/
   ```

3. **Copy and rename template files:**
   ```bash
   # Copy from template submodule
   cp .claude/templates/FeatureScreen.kt.template feature/profile/ui/src/main/java/com/mycompany/myapp/profile/ui/ProfileScreen.kt
   cp .claude/templates/FeatureViewModel.kt.template feature/profile/ui/src/main/java/com/mycompany/myapp/profile/ui/ProfileViewModel.kt
   # etc.
   ```

4. **Update settings.gradle.kts:**
   ```kotlin
   include(":feature:profile:ui")
   include(":feature:profile:navigation")
   ```

### 5. App Module 완전 설정

#### 5.1 MainActivity 설정

`app/src/main/java/{PACKAGE_PATH}/MainActivity.kt`:

```kotlin
package { PACKAGE_NAME }.{ APP_NAME }

import android . os . Bundle
  import androidx . activity . ComponentActivity
  import androidx . activity . compose . setContent
  import androidx . activity . enableEdgeToEdge
  import androidx . compose . foundation . layout . fillMaxSize
  import androidx . compose . foundation . layout . padding
  import androidx . compose . material3 . Scaffold
  import androidx . compose . ui . Modifier
  import dagger . hilt . android . AndroidEntryPoint
  import { PACKAGE_NAME }.{ APP_NAME }.navigation.{ APP_NAME_PASCAL } Navigation
  import { PACKAGE_NAME }.designsystem.theme.{ APP_NAME_PASCAL } Theme

  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      enableEdgeToEdge()

      setContent {
        { APP_NAME_PASCAL } Theme {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            { APP_NAME_PASCAL } Navigation (
              modifier = Modifier.padding(innerPadding)
              )
          }
        }
      }
    }
  }
```

#### 5.2 Application 클래스 설정

`app/src/main/java/{PACKAGE_PATH}/{APP_NAME_PASCAL}Application.kt`:

```kotlin
package { PACKAGE_NAME }.{ APP_NAME }

import android . app . Application
  import dagger . hilt . android . HiltAndroidApp

  @HiltAndroidApp
  class {APP_NAME_PASCAL } Application : Application () {
  override fun onCreate() {
    super.onCreate()
  }
}
```

#### 5.3 AndroidManifest.xml 설정

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">
  
  <application
    android:name=".{APP_NAME_PASCAL}Application"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.{APP_NAME_PASCAL}"
    tools:targetApi="31">
    
    <activity
      android:name=".MainActivity"
      android:exported="true"
      android:theme="@style/Theme.{APP_NAME_PASCAL}">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
      </intent-filter>
    </activity>
  </application>
</manifest>
```

### 6. Navigation 완전 설정

#### 6.1 메인 Navigation 생성

`app/src/main/java/{PACKAGE_PATH}/{APP_NAME}/navigation/{APP_NAME_PASCAL}Navigation.kt`:

```kotlin
package { PACKAGE_NAME }.{ APP_NAME }.navigation

import androidx . compose . runtime . Composable
  import androidx . compose . ui . Modifier
  import androidx . navigation . NavHostController
  import androidx . navigation . compose . NavHost
  import androidx . navigation . compose . rememberNavController
  import { PACKAGE_NAME }.navigation.NavigationDirections

@Composable
fun {
  APP_NAME_PASCAL
}Navigation(
modifier: Modifier = Modifier,
navController: NavHostController = rememberNavController()
) {
  NavHost(
    navController = navController,
    startDestination = NavigationDirections.Home, // 또는 첫 번째 화면
    modifier = modifier
  ) {
    // 홈 화면 (예시)
    home(
      navController = navController,
      onNavigateToProfile = {
        navController.navigate(NavigationDirections.Profile)
      }
    )

    // 프로필 화면 (예시)
    profile(
      navController = navController,
      onNavigateBack = {
        navController.popBackStack()
      }
    )

    // 추가 기능들을 여기에 등록
    // {FEATURE_NAME}(navController)
  }
}
```

#### 6.2 Navigation Directions 정의

`core/navigation/src/main/java/{PACKAGE_PATH}/navigation/NavigationDirections.kt`:

```kotlin
package { PACKAGE_NAME }.navigation

import kotlinx . serialization . Serializable

  @Serializable
  sealed class NavigationDirections {

    @Serializable
    data object Home : NavigationDirections()

    @Serializable
    data object Profile : NavigationDirections()

    @Serializable
    data class ProfileDetail(val userId: String) : NavigationDirections()

    // 추가 화면들...
  }
```

#### 6.3 기본 홈 화면 구현 예시

`feature/home/ui/src/main/java/{PACKAGE_PATH}/home/ui/HomeScreen.kt`:

```kotlin
package { PACKAGE_NAME }.home.ui

import androidx . compose . foundation . layout . *
  import androidx . compose . material3 . *
  import androidx . compose . runtime . Composable
  import androidx . compose . ui . Alignment
  import androidx . compose . ui . Modifier
  import androidx . compose . ui . unit . dp
  import androidx . hilt . navigation . compose . hiltViewModel

  @Composable
  fun HomeRoute(
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
  ) {
    HomeScreen(
      onNavigateToProfile = onNavigateToProfile
    )
  }

@Composable
fun HomeScreen(
  onNavigateToProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "Welcome to {APP_NAME_PASCAL}",
      style = MaterialTheme.typography.headlineMedium
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(onClick = onNavigateToProfile) {
      Text("Go to Profile")
    }
  }
}
```

### 7. Feature Integration with Main App

기능을 메인 앱과 통합하는 완전한 과정:

#### 7.1 build.gradle.kts 의존성 추가

```kotlin
// app/build.gradle.kts
dependencies {
  // 새 기능 추가
  implementation(project(":feature:{FEATURE_NAME}:navigation"))
}
```

#### 7.2 Navigation Graph에 기능 추가

```kotlin
// {APP_NAME_PASCAL}Navigation.kt에서
NavHost(...) {
  // 기존 화면들...

  // 새 기능 추가
  { FEATURE_NAME } Navigation (
    navController = navController,
  onNavigateBack = { navController.popBackStack() }
  )
}
```

## 📝 Customization Tips

1. **State Management**: Customize the State sealed interface based on your feature needs
2. **Navigation**: Add more screens to a feature by extending the Directions object
3. **Dependencies**: Add feature-specific dependencies in the build.gradle.kts files
4. **UI Components**: Create reusable components in the designsystem module
5. **Business Logic**: Implement use cases in the domain layer

## 🤖 Claude Code Agent Configuration

This template includes specialized AI agents to help with development. To use them in Claude Code:

### Available Agents

The `.claude/agents/` directory contains the following specialized agents:

| Agent                 | Purpose                                          | Use When                                                                 |
|-----------------------|--------------------------------------------------|--------------------------------------------------------------------------|
| `android-developer`   | Jetpack Compose & Android development specialist | Writing UI code, implementing screens, Android-specific features         |
| `designer`            | Design system architect                          | Creating UI components, establishing design tokens, ensuring consistency |
| `tech-lead`           | Architecture & refactoring expert                | Planning features, code architecture, refactoring legacy code            |
| `localization-expert` | Translation & L10N specialist                    | Adding string resources, multi-language support                          |
| `market-researcher`   | Market analysis & user research                  | Validating features, competitive analysis, user insights                 |
| `elon`                | Product visionary & PRD creator                  | Feature planning, product decisions, strategic direction                 |

### How to Add Agents to Claude Code

1. **Copy agent files to your project:**
   ```bash
   cp -r .claude/agents/ your-project/.claude/agents/
   ```

2. **Configure agents in Claude Code:**
    - Open your project in Claude Code
    - Run `/agents` command to see available agents
    - Use `/agent {agent-name}` to activate a specific agent

3. **Agent Usage Pattern:**
   ```
   /agent elon          # For feature planning and PRDs
   /agent designer      # For UI/UX design decisions  
   /agent android-developer # For implementation
   ```

### Recommended Workflow

1. **Feature Planning**: Start with `/agent elon` for product strategy
2. **Market Validation**: Use `/agent market-researcher` for validation
3. **Design**: Use `/agent designer` for UI/UX design
4. **Architecture**: Use `/agent tech-lead` for technical planning
5. **Implementation**: Use `/agent android-developer` for coding
6. **Localization**: Use `/agent localization-expert` for multi-language support

### Agent Integration Example

```bash
# 1. Plan a new feature
/agent elon
"새로운 사용자 프로필 기능을 추가하고 싶어"

# 2. Design the UI
/agent designer  
"프로필 화면의 UI 컴포넌트를 디자인해줘"

# 3. Implement the feature
/agent android-developer
"프로필 화면을 Jetpack Compose로 구현해줘"
```

## 🚨 일반적인 문제 해결

### App Module 관련 문제

#### 문제 1: "MainActivity를 찾을 수 없음"

**증상**: 앱 실행 시 `ClassNotFoundException` 발생
**해결책**:

```xml
<!-- AndroidManifest.xml에서 올바른 패키지 경로 확인 -->
<activity android:name=".MainActivity"/>
```

#### 문제 2: "Application 클래스 인식 안됨"

**증상**: Hilt 초기화 오류
**해결책**:

```xml
<!-- AndroidManifest.xml -->
<application android:name=".{APP_NAME_PASCAL}Application"/>
```

```kotlin
// Application 클래스에 @HiltAndroidApp 추가
@HiltAndroidApp
class {APP_NAME_PASCAL }Application : Application()
```

### Navigation 관련 문제

#### 문제 3: "Destination을 찾을 수 없음"

**증상**: `IllegalArgumentException: No destination with route`
**해결책**:

```kotlin
// 1. NavigationDirections가 올바르게 정의되었는지 확인
@Serializable
data object Home : NavigationDirections()

// 2. NavHost에 composable이 등록되었는지 확인
NavHost(...) {
  composable<NavigationDirections.Home> {
    HomeRoute(...)
  }
}
```

#### 문제 4: "Back stack이 비어있음"

**증상**: `popBackStack()` 시 앱 종료
**해결책**:

```kotlin
// Safe navigation 구현
onNavigateBack = {
  if (navController.previousBackStackEntry != null) {
    navController.popBackStack()
  } else {
    // 홈으로 이동 또는 앱 종료
    navController.navigate(NavigationDirections.Home) {
      popUpTo(NavigationDirections.Home) { inclusive = true }
    }
  }
}
```

### Build 관련 문제

#### 문제 5: "Duplicate class found"

**증상**: 빌드 시 중복 클래스 오류
**해결책**:

```kotlin
// build.gradle.kts에서 중복 의존성 제거
dependencies {
  // ❌ 잘못된 예시
  implementation(project(":feature:profile:ui"))
  implementation(project(":feature:profile:navigation"))

  // ✅ 올바른 예시 (navigation만 추가)
  implementation(project(":feature:profile:navigation"))
}
```

#### 문제 6: "Cannot resolve symbol"

**증상**: 임포트 오류
**해결책**:

```kotlin
// 1. settings.gradle.kts에 모듈이 포함되었는지 확인
include(":feature:profile:ui")
include(":feature:profile:navigation")

// 2. build.gradle.kts에 의존성이 추가되었는지 확인
implementation(project(":feature:profile:navigation"))
```

### Hilt 관련 문제

#### 문제 7: "Dagger component not found"

**증상**: Hilt 주입 실패
**해결책**:

```kotlin
// 1. MainActivity에 @AndroidEntryPoint 추가
@AndroidEntryPoint
class MainActivity : ComponentActivity()

// 2. ViewModel에 @HiltViewModel 추가
@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val repository: ProfileRepository
) : ContainerHost<ProfileState, ProfileSideEffect>
```

### Compose 관련 문제

#### 문제 8: "Recomposition loop"

**증상**: 무한 리컴포지션
**해결책**:

```kotlin
// ❌ 잘못된 예시
@Composable
fun Screen() {
  val state = remember { mutableStateOf(0) }
  state.value = state.value + 1 // 무한 리컴포지션!
}

// ✅ 올바른 예시
@Composable
fun Screen() {
  var state by remember { mutableStateOf(0) }

  Button(onClick = { state += 1 }) {
    Text("Count: $state")
  }
}
```

### 🎨 Compose Preview 설정

**중요**: Compose Preview가 작동하려면 다음 설정이 필요합니다:

1. **libs.versions.toml**에 Compose Compiler 버전 추가:
```toml
[versions]
androidxComposeCompiler = "1.5.15"  # Kotlin 버전과 호환되는 버전 사용
```

2. **AndroidCompose.kt**에 composeOptions 추가:
```kotlin
commonExtension.apply {
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.findVersion("androidxComposeCompiler").get().toString()
    }
}
```

3. **Preview 작성 예시**:
```kotlin
@Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun ScreenPreview() {
    AppTheme {
        MyScreen()
    }
}
```

## ✅ 템플릿 적용 검증 체크리스트

### 1. 프로젝트 구조 확인

- [ ] `settings.gradle.kts`에 모든 모듈 포함됨
- [ ] `app/build.gradle.kts`에 필요한 feature:navigation 의존성 추가됨
- [ ] 패키지 구조가 일관되게 설정됨 (`{PACKAGE_NAME}.{MODULE_NAME}`)

### 2. App Module 검증

- [ ] `MainActivity.kt` 생성 및 `@AndroidEntryPoint` 추가
- [ ] `{APP_NAME_PASCAL}Application.kt` 생성 및 `@HiltAndroidApp` 추가
- [ ] `AndroidManifest.xml`에서 Application 클래스 등록
- [ ] 메인 Navigation 컴포저블 생성

### 3. Navigation 검증

- [ ] `NavigationDirections.kt`에 모든 화면 경로 정의
- [ ] 각 feature의 navigation 함수가 NavGraphBuilder에 등록
- [ ] Type-safe navigation 사용 (Serializable objects)
- [ ] Back navigation 적절히 처리

### 4. Feature Module 검증

- [ ] UI 모듈에 Screen, ViewModel, Contract 파일들 생성
- [ ] Navigation 모듈에 Navigation, Directions 파일들 생성
- [ ] build.gradle.kts에 올바른 플러그인 적용
- [ ] 의존성이 올바르게 설정됨

### 5. Hilt 설정 검증

- [ ] Application 클래스에 `@HiltAndroidApp`
- [ ] MainActivity에 `@AndroidEntryPoint`
- [ ] ViewModel에 `@HiltViewModel`과 `@Inject constructor`
- [ ] Repository/UseCase에 `@Inject`나 Module 설정

### 6. Build 검증

```bash
# 빌드 성공 확인
./gradlew build

# 린트 검사
./gradlew lint

# 테스트 실행
./gradlew test
```

### 7. 런타임 검증

- [ ] 앱이 크래시 없이 실행됨
- [ ] 네비게이션이 정상적으로 작동함
- [ ] Back button 처리가 적절함
- [ ] State 관리가 정상적으로 작동함

## 🔧 디버깅 팁

### Logcat 활용

```kotlin
// 네비게이션 디버깅
Log.d("Navigation", "Navigating to: ${destination::class.simpleName}")

// Compose 리컴포지션 디버깅
Log.d("Compose", "Screen recomposed: ${System.currentTimeMillis()}")
```

### Android Studio 도구 활용

1. **Layout Inspector**: Compose 계층 구조 확인
2. **Database Inspector**: Room DB 상태 확인
3. **Network Inspector**: API 호출 모니터링
4. **Memory Profiler**: 메모리 누수 확인

### 자주 확인할 파일들

- `settings.gradle.kts`: 모듈 누락 확인
- `AndroidManifest.xml`: 컴포넌트 등록 확인
- `build.gradle.kts`: 의존성 및 플러그인 확인
- Navigation 파일: 경로 정의 및 등록 확인

## ⚠️ Important Notes

- Always follow the existing package structure
- Use Hilt for dependency injection
- Implement proper error handling in your ViewModels
- Follow Material Design 3 guidelines for UI
- Write unit tests for your ViewModels and use cases
- **Use agents proactively**: Start with product planning (elon) before jumping into implementation
- **Complete setup**: Ensure MainActivity, Application class, and AndroidManifest.xml are properly configured
- **Navigation consistency**: Always use type-safe navigation with proper back stack management

## 📚 Git Submodule Reference Guide

### Understanding the Template Submodule

The `.claude` directory in your project is a Git submodule that points to the android-template repository. This approach
provides several benefits:

1. **Version Control**: Template updates are tracked independently
2. **Consistency**: All projects use the same template source
3. **Easy Updates**: Pull latest template improvements with simple commands
4. **Clean Separation**: Template code is separate from project code

### Common Submodule Commands

#### Initial Setup

```bash
# Clone a project with submodules
git clone --recursive [project-url]

# If you forgot --recursive, initialize submodules after cloning
git submodule init
git submodule update

# Or combine init and update
git submodule update --init --recursive
```

#### Updating the Template

```bash
# Update to the latest template version
cd .claude
git fetch origin
git checkout main
git pull origin main

# Return to main project and commit the update
cd ..
git add .claude
git commit -m "Update android-template to latest version"
git push
```

#### Checking Submodule Status

```bash
# View current submodule status
git submodule status

# See which commit the submodule is pointing to
git ls-tree HEAD .claude

# Check for available updates
cd .claude
git fetch
git log HEAD..origin/main --oneline
```

#### Working with Specific Versions

```bash
# Use a specific template version/tag
cd .claude
git checkout v1.2.3  # or specific commit hash
cd ..
git add .claude
git commit -m "Pin template to version v1.2.3"

# Return to latest version
cd .claude
git checkout main
git pull origin main
```

#### Troubleshooting Submodules

**Problem: Submodule directory is empty**

```bash
# Initialize and update the submodule
git submodule update --init --recursive
```

**Problem: Submodule has local changes**

```bash
# Check what changed
cd .claude
git status
git diff

# Discard local changes (if unintended)
git reset --hard HEAD
git clean -fd

# Or stash changes temporarily
git stash
```

**Problem: Merge conflicts in submodule pointer**

```bash
# During a merge, if submodule conflicts occur
# Choose the version you want
git checkout --theirs .claude  # Use their version
# OR
git checkout --ours .claude     # Use our version

# Then update the submodule
git submodule update --init
```

### Template Development Workflow

If you need to modify the template for all projects:

1. **Fork the template repository**
   ```bash
   # Fork https://github.com/hsikkk/android-template on GitHub
   ```

2. **Update submodule URL in your project**
   ```bash
   # Change submodule URL to your fork
   git submodule set-url .claude https://github.com/[your-username]/android-template.git
   ```

3. **Make changes in the submodule**
   ```bash
   cd .claude
   git checkout -b feature/my-improvement
   # Make your changes
   git add .
   git commit -m "Improve template feature"
   git push origin feature/my-improvement
   ```

4. **Create a pull request** to the original template repository

### Best Practices

1. **Regular Updates**: Periodically update the template to get improvements
   ```bash
   git submodule update --remote .claude
   ```

2. **Document Template Version**: In your project README, note which template version you're using
   ```markdown
   Template Version: android-template v2.0.0 (commit: abc123)
   ```

3. **Don't Modify Template Files Directly**: Copy template files to your project before modifying
   ```bash
   # Good: Copy then modify
   cp .claude/templates/FeatureScreen.kt.template feature/myfeature/Screen.kt
   
   # Bad: Modifying directly in .claude/
   # This creates uncommitted changes in the submodule
   ```

4. **Commit Submodule Updates Separately**: Keep template updates in separate commits for clarity
   ```bash
   git add .claude
   git commit -m "chore: Update android-template to latest version"
   ```

5. **CI/CD Configuration**: Ensure your CI/CD pipeline initializes submodules
   ```yaml
   # Example: GitHub Actions
   - name: Checkout code
     uses: actions/checkout@v2
     with:
       submodules: recursive
   ```

### Template File Application Script

For convenience, you can create a script to apply templates:

```bash
#!/bin/bash
# apply-template.sh

TEMPLATE_DIR=".claude/templates"
FEATURE_NAME=$1
PACKAGE_NAME=$2

if [ -z "$FEATURE_NAME" ] || [ -z "$PACKAGE_NAME" ]; then
    echo "Usage: ./apply-template.sh [feature-name] [package-name]"
    exit 1
fi

# Copy and process templates
cp $TEMPLATE_DIR/FeatureScreen.kt.template temp.kt
sed -i "s/{FEATURE_NAME}/$FEATURE_NAME/g" temp.kt
sed -i "s/{PACKAGE_NAME}/$PACKAGE_NAME/g" temp.kt
# ... continue for other replacements

echo "Template applied for feature: $FEATURE_NAME"
```

This comprehensive guide ensures you can effectively use the template as a submodule in your Android projects.
