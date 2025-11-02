# Core Domain Module

Domain 레이어는 애플리케이션의 비즈니스 로직을 담당하는 순수한 Kotlin 모듈입니다.

## 구조

```
core:domain/
├── model/          # 도메인 모델 (Entity)
├── repository/     # Repository 인터페이스
└── usecase/        # 비즈니스 로직 (UseCase)
```

## 주요 특징

- **순수 Kotlin**: Android 의존성이 없는 순수한 비즈니스 로직
- **단일 책임**: 각 UseCase는 하나의 비즈니스 로직만 담당
- **의존성 역전**: Repository 인터페이스를 통한 Data 레이어 분리
- **테스트 용이**: Mock 객체를 사용한 단위 테스트 가능

## 사용 방법

### 1. Model 정의
```kotlin
data class User(
    val id: Long,
    val name: String,
    val email: String
)
```

### 2. Repository 인터페이스 정의
```kotlin
interface UserRepository {
    suspend fun getUser(id: Long): User?
    suspend fun saveUser(user: User)
}
```

### 3. UseCase 구현
```kotlin
class GetUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: Long): Result<User> {
        return repository.getUser(userId)?.let {
            Result.success(it)
        } ?: Result.failure(UserNotFoundException())
    }
}
```

## 의존성

- Kotlin Coroutines: 비동기 처리