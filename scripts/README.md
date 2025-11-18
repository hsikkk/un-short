# Lifetime Premium 프로모션 코드 생성 가이드

## 개요

un:short 앱의 Lifetime Premium을 지인들에게 선물하기 위한 프로모션 코드 생성 도구입니다.

## 코드 생성 방법

### 방법 1: Android Studio에서 실행

1. `PromoCodeGenerator.kt` 파일을 Android Studio에서 엽니다
2. `main()` 함수 옆의 ▶️ 버튼을 클릭
3. Run Configuration에서 Program arguments 설정:
   - 예시: `10 FRIEND` (10개의 코드, FRIEND 접두사)
4. 실행하면 콘솔에 코드가 출력되고 CSV 파일이 생성됩니다

### 방법 2: Kotlin REPL 사용

```kotlin
import PromoCodeGenerator

// 5개 생성
val codes = PromoCodeGenerator.generateCodes(5, "FRIEND")
codes.forEach { println(it.code) }

// CSV로 저장
val csv = PromoCodeGenerator.toCsv(codes)
println(csv)
```

### 방법 3: 직접 코드 생성

```kotlin
import com.muuu.unshort.promo.PromoCodeValidator

// 특정 ID로 코드 생성
val code = PromoCodeValidator.generateCode("GIFT001A1B2")
println(code) // UNSHORT-GIFT001A1B2-XXXXXXXX
```

## 생성된 코드 예시

```
UNSHORT-FRIEND001X4Y2-A3F9C8D2
UNSHORT-FRIEND002Z9K5-B8E2D1F3
UNSHORT-FRIEND003M7N3-C4A8B6E9
UNSHORT-FRIEND004P2Q8-D7C3F2A5
UNSHORT-FRIEND005R6S1-E9B4A8C7
```

## 코드 형식

- **형식**: `UNSHORT-{ID}-{SIGNATURE}`
- **ID**: 사용자 정의 가능 (영문 대문자, 숫자)
- **SIGNATURE**: HMAC-SHA256 서명 (8자리 HEX)

## 보안

- 코드는 앱 내부 비밀키로 서명됨
- 서버 없이 로컬에서 검증 가능
- 1회용 (한 번 사용하면 SharedPreferences에 기록)

## 배포 방법

1. 위 방법 중 하나로 프로모션 코드 생성
2. 생성된 CSV 파일 또는 코드를 안전하게 보관
3. 지인들에게 코드를 개별적으로 전달
4. 수신자는 앱 설정에서 "코드 입력" 버튼 클릭하여 활성화

## 주의사항

- 소량(5~10개) 생성 권장
- 코드 유출 시 누구나 사용 가능하므로 안전하게 관리
- 이미 사용된 코드는 재사용 불가 (기기에 저장됨)
- 프리미엄 활성화 후에는 "코드 입력" 버튼이 사라집니다

## 테스트

```kotlin
// 테스트 코드 생성
val testCode = PromoCodeValidator.generateCode("TEST001")
println(testCode) // UNSHORT-TEST001-XXXXXXXX

// 검증
val result = PromoCodeValidator.validate(testCode)
// result: ValidationResult.Valid
```

## 문제 해결

### 코드가 유효하지 않다고 나올 때
- 코드 형식 확인 (대소문자, 하이픈)
- 복사 시 공백이 포함되지 않았는지 확인

### 이미 Premium인데 버튼이 보일 때
- 앱 재시작
- PremiumManager가 올바르게 초기화되었는지 확인
