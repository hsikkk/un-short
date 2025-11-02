# Clean Architecture 가이드

## 개요
이 가이드는 Android 프로젝트에서 Clean Architecture를 적용하는 방법을 설명합니다. 특히 순환 참조를 방지하고 명확한 의존성 계층을 유지하는 방법에 중점을 둡니다.

## 모듈 구조 및 의존성 계층

### Layer 1: 독립적인 모듈들 (의존성 없음)
#### core:domain
- **역할**: 순수 비즈니스 로직
- **포함 내용**: Model, Repository 인터페이스, UseCase
- **의존성**: 없음 (순수 Kotlin)
- **예시 구조**:
  ```
  domain/
  ├── model/         # 도메인 엔티티
  ├── repository/    # Repository 인터페이스
  └── usecase/       # 비즈니스 로직
  ```

#### core:common
- **역할**: 공통 유틸리티
- **포함 내용**: Extension functions, Constants, Utils
- **의존성**: 없음

#### core:designsystem
- **역할**: UI 컴포넌트 및 테마
- **포함 내용**: Compose UI 컴포넌트, Theme, Typography
- **의존성**: Compose 라이브러리만

### Layer 2: 데이터 소스 모듈들
#### core:database
- **역할**: 로컬 데이터베이스
- **포함 내용**: Room Entity, DAO, Database
- **의존성**: core:common

#### core:datastore
- **역할**: Key-Value 저장소
- **포함 내용**: DataStore, Preferences
- **의존성**: core:common

#### core:network
- **역할**: 네트워크 통신
- **포함 내용**: Retrofit Service, DTO
- **의존성**: core:common

### Layer 3: Repository 구현
#### core:data
- **역할**: Repository 구현체
- **포함 내용**: Repository 구현, Mapper, Adapter
- **의존성**: core:domain, core:database, core:datastore, core:network
- **Adapter 패턴 예시**:
  ```kotlin
  class UserRepositoryImpl(
      private val userDao: UserDao,
      private val userApi: UserApi
  ) : UserRepository { // domain의 인터페이스 구현
      override suspend fun getUser(id: Long): User {
          // Entity -> Domain Model 변환
          return userDao.getUser(id).toDomainModel()
      }
  }
  ```

### Layer 4: DI 컨테이너
#### core:di
- **역할**: 의존성 주입 관리
- **포함 내용**: AppContainer, Module
- **의존성**: core:domain, core:data, core:database, core:datastore
- **순환 참조 방지**:
  ```kotlin
  class AppContainer(context: Context) {
      // Repository와 UseCase만 제공
      val userRepository: UserRepository = ...
      val getUserUseCase: GetUserUseCase = ...

      // Service나 ViewModel은 app 모듈에서 직접 관리
      // 이렇게 하면 di ↔ service 순환 참조 방지
  }
  ```

### Layer 5: 프레젠테이션 관련
#### core:navigation
- **역할**: 화면 전환 관리
- **의존성**: core:common

#### core:viewmodel
- **역할**: ViewModel 베이스 클래스
- **의존성**: core:domain

### Layer 6: Android 컴포넌트
#### core:service
- **역할**: Android Service, BroadcastReceiver
- **의존성**: core:domain, core:di

#### feature 모듈들
- **역할**: 각 기능별 UI
- **의존성**: core:domain, core:designsystem, core:viewmodel, core:navigation

### app 모듈
- **역할**: 애플리케이션 진입점, 모든 모듈 통합
- **의존성**: 모든 모듈
- **책임**:
  - Application 클래스 관리
  - ViewModel 생성 (ViewModelFactory)
  - Service 초기화
  - Navigation 그래프 구성

## 순환 참조 방지 전략

### 문제 상황
```
❌ 잘못된 예:
core:di → core:service (di가 service 의존)
core:service → core:di (service가 di 의존)
→ 순환 참조 발생!
```

### 해결 방법
```
✅ 올바른 예:
1. core:di는 domain, data만 의존
2. core:service는 domain, di 의존
3. app 모듈에서 service 초기화 시 필요한 의존성 주입
```

### 구체적인 구현
```kotlin
// core:di/AppContainer.kt
class AppContainer(context: Context) {
    val userRepository: UserRepository = ...
    // Service는 포함하지 않음
}

// core:service/MyService.kt
class MyService : Service() {
    // 의존성은 app 모듈에서 주입받음
    lateinit var repository: UserRepository
}

// app/Application.kt
class MyApplication : Application() {
    val appContainer = AppContainer.getInstance(this)

    override fun onCreate() {
        // Service에 의존성 주입
        MyService.repository = appContainer.userRepository
    }
}
```

## UseCase 패턴

### 목적
- 단일 책임 원칙: 하나의 UseCase는 하나의 비즈니스 로직만 담당
- 테스트 용이성: Mock을 사용한 단위 테스트 가능
- 재사용성: 여러 ViewModel에서 같은 UseCase 사용 가능

### 구현 예시
```kotlin
// domain/usecase/GetUserUseCase.kt
class GetUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: Long): Result<User> {
        return try {
            val user = repository.getUser(userId)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// feature/viewmodel/UserViewModel.kt
class UserViewModel(
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {
    fun loadUser(id: Long) {
        viewModelScope.launch {
            getUserUseCase(id)
                .onSuccess { user ->
                    // UI 상태 업데이트
                }
                .onFailure { error ->
                    // 에러 처리
                }
        }
    }
}
```

## 테스트 전략

### Domain 레이어 테스트
```kotlin
class GetUserUseCaseTest {
    @Test
    fun `사용자 조회 성공`() = runTest {
        // Given
        val repository = mockk<UserRepository>()
        val useCase = GetUserUseCase(repository)

        coEvery { repository.getUser(1) } returns User(1, "John")

        // When
        val result = useCase(1)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("John", result.getOrNull()?.name)
    }
}
```

### Repository 테스트
```kotlin
class UserRepositoryImplTest {
    @Test
    fun `로컬 데이터 조회`() = runTest {
        // Given
        val dao = mockk<UserDao>()
        val api = mockk<UserApi>()
        val repository = UserRepositoryImpl(dao, api)

        // When & Then
        // ...
    }
}
```

## 마이그레이션 가이드

### 기존 프로젝트에 Clean Architecture 적용하기

1. **Domain 모듈 생성**
   - Model 클래스 이동
   - Repository 인터페이스 추출
   - UseCase 작성

2. **Data 레이어 분리**
   - Repository 구현체를 data 모듈로 이동
   - Mapper 작성 (Entity ↔ Model)

3. **DI 구조 개선**
   - AppContainer 생성
   - 순환 참조 확인 및 제거

4. **점진적 마이그레이션**
   - 한 번에 한 feature씩 마이그레이션
   - 테스트 작성으로 안정성 확보

## 체크리스트

- [ ] Domain 모듈이 Android 의존성 없이 순수 Kotlin으로 작성되었는가?
- [ ] 각 UseCase가 단일 책임을 가지는가?
- [ ] Repository 인터페이스가 domain에, 구현체가 data에 있는가?
- [ ] 모듈 간 순환 참조가 없는가?
- [ ] DI Container가 올바르게 구성되었는가?
- [ ] 각 레이어가 적절한 테스트를 가지고 있는가?