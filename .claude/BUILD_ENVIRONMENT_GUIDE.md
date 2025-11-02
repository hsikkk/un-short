# Android Build Environment Guide

이 가이드는 Android 템플릿 프로젝트의 빌드 환경 설정과 최신 의존성 정보를 제공합니다.

## 🚀 시스템 요구사항

### 필수 도구

| Tool | 최소 버전 | 권장 버전 | 설명 |
|------|----------|----------|------|
| **Android Studio** | 2024.2.1 (Ladybug) | 2024.3.2 (Meerkat Feature Drop) | IDE |
| **Java/JDK** | JDK 17 | JDK 21 | Kotlin 2.0+ 지원 |
| **Gradle** | 8.6 | 8.10+ | 빌드 시스템 |
| **Android SDK** | API 24 | API 35 | 최소/타겟 SDK |
| **Git** | 2.30+ | 최신 | 버전 관리 |

### 하드웨어 권장사항

- **RAM**: 최소 8GB, 권장 16GB+ (Gradle 빌드 캐시용)
- **Storage**: 최소 20GB 여유 공간 (Android SDK, 빌드 아티팩트)
- **CPU**: 멀티코어 프로세서 (병렬 빌드)

## 🔧 환경 설정

### 1. JDK 설정

```bash
# JDK 21 설치 확인
java -version
javac -version

# Android Studio에서 JDK 경로 설정
# File > Project Structure > SDK Location > JDK Location
```

### 2. Android SDK 설정

```bash
# SDK Manager에서 설치해야 할 항목들:
# - Android API 35 (Android 15)
# - Android SDK Build-Tools 35.0.0+
# - Android Emulator
# - Android SDK Platform-Tools
# - Android SDK Tools
```

### 3. Gradle 설정

프로젝트의 `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https://services.gradle.org/distributions/gradle-8.10-bin.zip
```

## 📦 의존성 버전 정보 (2024년 9월 기준)

### 핵심 빌드 도구

```toml
[versions]
# Build tools
android-gradle-plugin = "8.7.2"    # AGP 최신 안정 버전
kotlin = "2.0.21"                   # Kotlin 2.0+ (Compose Compiler 통합)
ksp = "2.0.21-1.0.25"              # Kotlin Symbol Processing
```

### AndroidX 라이브러리

```toml
# AndroidX Core
core-ktx = "1.13.1"                # AndroidX Core
lifecycle-runtime-ktx = "2.8.6"    # Lifecycle
activity-compose = "1.9.2"         # Activity Compose
```

### Jetpack Compose

```toml
# Compose
compose-bom = "2024.09.03"         # Compose BOM (Bill of Materials)
material3 = "1.3.0"               # Material Design 3
```

### 의존성 주입 & 내비게이션

```toml
# Hilt
hilt = "2.52"                      # Dagger Hilt (KSP 지원)
hilt-navigation-compose = "1.2.0"  # Hilt Navigation Compose

# Navigation
navigation-compose = "2.8.1"       # Type-safe Navigation
```

### 상태 관리 & 기타 라이브러리

```toml
# MVI Pattern
orbit = "9.0.0"                    # Orbit MVI

# Async & Collections
coroutine = "1.9.0"               # Kotlin Coroutines
kotlinx-collections-immutable = "0.3.8"  # Immutable Collections

# Image Loading
coil = "2.7.0"                    # Coil Compose

# Media
media3 = "1.4.1"                  # AndroidX Media3
```

## 🏗️ 빌드 구성

### Gradle 성능 최적화

`gradle.properties` 설정:
```properties
# 메모리 설정
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options=-Xmx2g

# 성능 최적화
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configuration-cache=true
org.gradle.vfs.watch=true

# Kotlin 설정
kotlin.experimental.tryK2=true
kotlin.daemon.jvmargs=-Xmx2g

# Android 최적화
android.useAndroidX=true
android.enableJetifier=false
android.nonTransitiveRClass=true
```

### 주요 변경사항

#### Kotlin 2.0+ 마이그레이션
- **Compose Compiler 통합**: 별도 버전 관리 불필요
- **K2 모드**: `kotlin.experimental.tryK2=true`로 활성화
- **KSP 권장**: KAPT 대신 KSP 사용으로 빌드 속도 2배 향상

#### Android Gradle Plugin 8.7+
- **Configuration Cache**: 빌드 속도 대폭 개선
- **Non-transitive R classes**: APK 크기 감소
- **Resource shrinking**: 미사용 리소스 자동 제거

#### Compose BOM 2024.09+
- **Material3 1.3**: Adaptive layouts 안정화
- **Type-safe Navigation**: Navigation 2.8+ 타입 안전 내비게이션
- **Performance**: Recomposition 최적화

## 🔍 호환성 매트릭스

| Kotlin | AGP | Gradle | JDK | Android Studio |
|--------|-----|--------|-----|---------------|
| 2.0.21 | 8.7.2 | 8.10 | 21 | 2024.3.2 |
| 2.0.20 | 8.6+ | 8.6+ | 17+ | 2024.2.1+ |
| 1.9.24 | 8.0+ | 8.0+ | 17+ | 2024.1.1+ |

## 🛠️ 빌드 명령어

### 일반적인 빌드 작업

```bash
# 프로젝트 빌드
./gradlew build

# 디버그 APK 생성
./gradlew assembleDebug

# 릴리즈 APK 생성
./gradlew assembleRelease

# 테스트 실행
./gradlew test

# 린트 검사
./gradlew lint

# 의존성 확인
./gradlew dependencies

# 빌드 캐시 정리
./gradlew clean
```

### 고급 빌드 작업

```bash
# 병렬 빌드 (더 빠른 빌드)
./gradlew build --parallel

# 빌드 캐시 활성화
./gradlew build --build-cache

# Configuration cache 사용
./gradlew build --configuration-cache

# 프로파일링
./gradlew build --profile
```

## 🐛 일반적인 문제 해결

### 빌드 오류

**문제**: `Could not resolve all dependencies for configuration`
```bash
# 해결: Gradle 캐시 정리
./gradlew clean
./gradlew --stop
rm -rf ~/.gradle/caches
```

**문제**: `Kotlin compilation failed`
```bash
# 해결: Kotlin Daemon 재시작
./gradlew --stop
./gradlew clean build
```

**문제**: `Insufficient memory for the JVM`
```bash
# 해결: gradle.properties 메모리 설정 확인
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
```

### 성능 문제

**느린 빌드 속도**:
1. `org.gradle.parallel=true` 활성화
2. `org.gradle.caching=true` 활성화
3. `org.gradle.configuration-cache=true` 활성화
4. KSP 사용 (KAPT 대신)
5. RAM 증설 고려

**높은 메모리 사용량**:
1. `kotlin.daemon.jvmargs` 조정
2. 불필요한 모듈 제거
3. Gradle daemon 정기적 재시작

## 🔄 의존성 업데이트

### 정기적인 업데이트 체크

```bash
# Gradle 래퍼 업데이트
./gradlew wrapper --gradle-version=8.10

# 의존성 업데이트 체크 (gradle-versions-plugin 사용 시)
./gradlew dependencyUpdates
```

### 안전한 업데이트 절차

1. **테스트 환경에서 먼저 검증**
2. **호환성 매트릭스 확인**
3. **점진적 업데이트** (한 번에 하나씩)
4. **빌드 및 테스트 실행**
5. **성능 비교**

## 📊 성능 벤치마크

### 빌드 시간 목표 (일반적인 프로젝트)

| 작업 | 목표 시간 | 설정 |
|------|----------|------|
| Clean Build | < 2분 | 16GB RAM, SSD, 8코어 |
| Incremental Build | < 30초 | Configuration cache 활성화 |
| Hot Reload (Compose) | < 5초 | K2 모드 활성화 |

### 최적화 체크리스트

- [ ] Configuration cache 활성화
- [ ] Parallel builds 활성화
- [ ] Build cache 활성화
- [ ] KSP 사용 (KAPT 대신)
- [ ] K2 모드 활성화
- [ ] 불필요한 의존성 제거
- [ ] ProGuard/R8 최적화 적용

## 📚 추가 리소스

- [Android Developer Documentation](https://developer.android.com)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [Android Studio Release Notes](https://developer.android.com/studio/releases)

---

이 가이드는 2024년 9월 기준으로 작성되었으며, 정기적으로 업데이트됩니다. 최신 정보는 공식 문서를 참조하세요.