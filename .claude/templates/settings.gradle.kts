pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "{{projectName}}"
include(":app")

// Core modules - 순환 참조 방지를 위한 의존성 계층 구조
// Layer 1: 독립적인 모듈들
include(":core:domain")        // 순수 비즈니스 로직 (의존성 없음)
include(":core:common")        // 공통 유틸리티 (의존성 없음)
include(":core:designsystem")  // UI 컴포넌트 (의존성 없음)

// Layer 2: 데이터 관련 모듈들
include(":core:database")      // Room 데이터베이스 (common에만 의존)
include(":core:datastore")     // DataStore (common에만 의존)
include(":core:network")       // 네트워크 클라이언트 (common에만 의존)

// Layer 3: Repository 구현
include(":core:data")          // Repository 구현 (domain, database, datastore, network에 의존)

// Layer 4: DI 컨테이너
include(":core:di")            // 의존성 주입 (domain, data, database, datastore에 의존)

// Layer 5: 프레젠테이션 관련
include(":core:navigation")    // 네비게이션 (common에만 의존)
include(":core:viewmodel")     // ViewModel 베이스 (domain에만 의존)

// Layer 6: Android 서비스
include(":core:service")       // Android 서비스 (domain, di에 의존)

// Feature modules - 필요에 따라 추가
// include(":feature:home")
// include(":feature:settings")
// include(":feature:detail")