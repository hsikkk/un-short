# un:short - 쇼츠 폼 차단 앱

YouTube Shorts, Instagram Reels, TikTok 등 중독성 있는 쇼츠 콘텐츠를 볼 때 **폰을 뒤집어서 30초 기다리게** 만드는 안드로이드 앱입니다.

## 핵심 기능

- **폰 뒤집기 챌린지**: 쇼츠 앱 실행 시 폰을 뒤집어서 30초 대기해야 함
- **실시간 감지**: Accessibility Service로 쇼츠/릴스 화면 자동 감지
- **가속도계 활용**: 센서로 폰이 뒤집어졌는지 실시간 확인
- **차단 대상 앱**:
  - YouTube Shorts
  - Instagram Reels
  - TikTok

## 작동 방식

1. 사용자가 YouTube, Instagram, TikTok 앱 실행
2. 쇼츠/릴스 화면 진입 시 앱이 자동 감지
3. 전체 화면 오버레이 표시
4. **폰을 뒤집어야** 30초 타이머 시작
5. 폰을 다시 세우면 타이머 일시정지
6. 30초 완료 시 오버레이 해제

## 기술 스택

- **Kotlin** - 메인 언어
- **Accessibility Service** - 앱 화면 감지
- **SensorManager** - 가속도계로 폰 방향 감지
- **WindowManager** - 시스템 오버레이
- **Material Design** - UI

## 설치 및 실행

### 1. 프로젝트 빌드

```bash
./gradlew assembleDebug
```

### 2. APK 설치

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 권한 설정

앱 실행 후 다음 권한 활성화 필요:

1. **접근성 서비스**: 설정 > 접근성 > ShortBlock 활성화
2. **다른 앱 위에 그리기**: 설정 > 특수 앱 액세스 > ShortBlock 허용

## 프로젝트 구조

```
app/src/main/
├── java/com/shortblock/
│   ├── MainActivity.kt              # 메인 액티비티 (설정 화면)
│   ├── ShortsBlockService.kt        # Accessibility Service (앱 감지)
│   ├── BlockOverlay.kt              # 차단 오버레이 UI + 타이머
│   └── FlipDetector.kt              # 가속도계 센서 처리
├── res/
│   ├── layout/
│   │   ├── activity_main.xml        # 메인 화면 레이아웃
│   │   └── overlay_flip_phone.xml   # 오버레이 레이아웃
│   ├── values/
│   │   └── strings.xml              # 문자열 리소스
│   └── xml/
│       └── accessibility_service_config.xml  # 접근성 서비스 설정
└── AndroidManifest.xml
```

## 주요 클래스 설명

### FlipDetector.kt
- 가속도계 센서로 폰 방향 감지
- Z축 가속도 < -8.0 = 폰이 뒤집힌 상태
- 리스너 패턴으로 상태 변화 전달

### BlockOverlay.kt
- WindowManager로 시스템 레벨 오버레이 생성
- CountDownTimer로 30초 카운트다운
- FlipDetector와 연동하여 폰이 뒤집혔을 때만 타이머 진행

### ShortsBlockService.kt
- AccessibilityService 상속
- YouTube/Instagram/TikTok 앱 모니터링
- View ID와 텍스트 기반으로 쇼츠 화면 감지
- 감지 시 BlockOverlay 표시

## 개선 아이디어

### 차단 강화
- [ ] 동기부여 영상 강제 시청
- [ ] 팔굽혀펴기/스쿼트 동작 인식
- [ ] 친구에게 알림 (공개 처형)
- [ ] 목표 실패 시 기부금 차감

### 동기부여
- [ ] 절약한 시간 통계
- [ ] 스트릭(연속 달성) 시스템
- [ ] 뱃지 및 레벨 시스템
- [ ] 친구와 경쟁 모드

### UX 개선
- [ ] 차단 강도 선택 (Soft/Medium/Hard)
- [ ] 시간대별 차단 설정
- [ ] 앱별 개별 설정
- [ ] 다크 모드

## 주의사항

- **접근성 권한**은 민감한 권한이므로 사용자 동의 필수
- 앱 업데이트 시 View ID가 변경될 수 있어 지속적인 유지보수 필요
- 완벽한 차단은 불가능 (사용자가 서비스 비활성화 가능)

## 라이선스

MIT License
