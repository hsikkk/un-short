# PRD: 피드 기반 앱 스크롤 차단 (Feed Block)

> 이 문서는 기능 개발 당시의 요구사항과 결정 기록입니다. 현재 동작은 [현재 제품 스펙](../SPEC.md)을 기준으로 합니다.

| 항목 | 값 |
|---|---|
| **문서 버전** | v1.3 |
| **작성일** | 2026-04-28 |
| **최종 수정일** | 2026-08-02 |
| **작성자** | hsikkk |
| **상태** | Implemented (Beta) |
| **관련 구현** | `app/src/main/java/com/muuu/unshort/feedblock/` |
| **릴리스 상태** | 현재 앱에 Beta 기능으로 포함 |

---

## 1. 개요

### 1.1 배경

un:short는 현재 YouTube Shorts, Instagram Reels, Facebook Reels, Naver Shorts, TikTok 등 **쇼츠(전체화면 세로 영상)** 중독 차단에 특화된 앱이다. 그러나 사용자 피드백과 자체 분석 결과, **쇼츠를 차단해도 Instagram 홈피드, YouTube 홈피드 등 일반 피드 무한 스크롤로 시간을 낭비하는 패턴**이 확인되었다.

쇼츠는 콘텐츠 단위가 명확(전체화면 1개 영상)하여 진입 즉시 차단이 가능했지만, 피드는 다음과 같은 차이로 동일 전략을 적용할 수 없다.

| 구분 | 쇼츠 | 피드 |
|---|---|---|
| 콘텐츠 단위 | 전체화면 1개 영상 | RecyclerView 내 다중 카드 |
| 진입 시점 차단 | 적절 | 부적절 (정보 탐색 목적도 있음) |
| 스크롤 감지 | 콘텐츠 해시 변경 | 누적 스크롤 거리 / 시간 |
| 미디어 정지 | 필수 | 대부분 불필요 |

### 1.2 문제 정의

**Problem Statement**: un:short 사용자는 쇼츠 차단 후에도 Instagram 홈피드, YouTube 홈피드 등을 무한 스크롤하며 의도하지 않은 시간 낭비를 경험한다. 그러나 피드는 정보 탐색의 정당한 목적도 있어, 쇼츠처럼 진입 즉시 차단하는 강한 차단 정책은 사용자 경험을 해친다.

### 1.3 핵심 가설

> 사용자가 피드에서 **누적 스크롤 거리/시간이 일정 임계값을 초과**하면 의도적 탐색이 아닌 **무의식적 스크롤**일 가능성이 높다. 이 시점에 30초 타이머와 의도 확인 인터럽트를 제공하면 의식적 사용 전환을 유도할 수 있다.

PoC를 통해 **임계값 도달 후 차단 오버레이 자동 호출**까지의 기술적 구현 가능성을 검증 완료(2026-04-28).

---

## 2. 목표 및 성공 지표

### 2.1 비즈니스 목표

- 기존 un:short 사용자 retention 강화 (쇼츠 차단만으로 부족하다는 사용자 이탈 방지)
- "무의식적 스크롤" 카테고리로 차단 영역 확장하여 제품 가치 제안 강화
- 베타 단계에서 정성 피드백 수집 후 정식 기능 승격 판단

### 2.2 사용자 가치

- 쇼츠뿐 아니라 일반 피드 무한 스크롤에서도 의식적 사용 전환 지원
- 단순 차단이 아닌 **임계값 기반 인터럽트**로 정보 탐색 사용성 보존
- 사용자가 직접 차단 정책 강도(시간/거리)를 조절 가능

### 2.3 성공 지표 (Beta 출시 후 4주 측정)

| 지표 | 정의 | 목표 |
|---|---|---|
| **베타 토글 활성화율** | 고급 기능 토글 ON / 전체 활성 사용자 | ≥ 15% |
| **차단 오버레이 도달률** | 피드 차단 트리거 / 피드 진입 세션 | 30~50% (낮으면 임계값↓, 높으면 임계값↑) |
| **그만보기 선택률** | 그만보기 클릭 / 차단 오버레이 표시 | ≥ 40% |
| **베타 기능 비활성화율** | 토글 OFF로 되돌린 사용자 / 활성화한 사용자 | ≤ 30% |
| **크래시 무관성** | 피드 차단 토글 ON 시 크래시율 증가폭 | < 0.05% |

### 2.4 Non-Goals (이 PRD에서 다루지 않음)

- 피드 사용량 통계 화면 (별도 PRD 또는 후속 릴리스)
- 일일 사용 제한 통합 (쇼츠 일일제한과는 별도 관리, 통합 시점은 후속 검토)
- 적응형 임계값 (사용 패턴 학습) - 단순 임계값 + 사용자 튜닝으로 충분 검증 후 검토

---

## 3. 페르소나 / 타겟 사용자

### 3.1 Primary Persona: "쇼츠는 끊었지만 피드는 못 끊은 J"

- **프로필**: 27세 직장인, un:short 3개월차 사용자
- **현재 상태**: YouTube Shorts/Reels 차단으로 쇼츠 시청은 줄었지만, **Instagram 홈피드를 무의식적으로 30분~1시간 스크롤**하는 패턴이 잔존
- **니즈**: "피드 자체를 못 보게 하는 건 부담. 너무 오래 보고 있을 때만 알려줬으면"
- **현재 우회**: un:short를 일시 해제하고 Instagram 사용 → 다시 켜기를 반복

### 3.2 Secondary Persona: 신규 잠재 사용자는 본 PRD 범위 외

기존 un:short 사용자에게 베타로 노출하여 학습 후, 신규 사용자 마케팅은 후속 단계.

---

## 4. 범위 (Scope)

### 4.1 In Scope

| Phase | 대상 앱 | 패키지명 | 출시 시점 |
|---|---|---|---|
| **Phase 1** | Instagram 홈피드 | `com.instagram.android` | v2.0 Beta |
| **Phase 2** | YouTube 홈피드 (쇼츠 외) | `com.google.android.youtube` | Phase 1 안정화 후 |

### 4.2 Out of Scope (본 PRD)

- Threads, X(Twitter), Facebook 뉴스피드, 네이버 메인피드 (별도 PRD)
- 피드 사용량 통계 / 차트 화면
- 쇼츠 일일제한과의 통합 관리
- 적응형 임계값 학습
- iOS 버전 (Android only)
- 위젯/홈화면 단축 토글

---

## 5. 차단 정책

### 5.1 트리거 정책 (단순 임계값 + 사용자 커스터마이징)

다음 두 임계값 중 **먼저 도달**하면 차단 오버레이를 표시한다.

| 임계값 | 기본값 | 조정 가능 범위 | 비고 |
|---|---|---|---|
| **누적 스크롤 거리** | 화면 높이 × 5배 | 3~10배 (1배 단위) | PoC 검증 시 약 14,775px 기준 |
| **누적 체류 시간** | 3분 | 1~10분 (1분 단위) | 피드 진입 후 경과 시간 |

**기본값 산정 근거** (PoC 측정 기반):
- 화면 높이 × 5배 ≈ 일반 사용자가 약 5~7개 카드를 본 시점
- 3분 ≈ 평균 피드 무의식 스크롤 세션의 단기 임계값

### 5.2 차단 흐름 (쇼츠와 동일한 패턴 + 강제 종료)

```
피드 진입 (Instagram 홈피드)
  ↓
ScrollAccumulator 세션 시작
  ↓
스크롤 누적 측정 (delta 합산 + 경과 시간)
  ↓
[임계값 도달]
  ↓
차단 오버레이 표시 (FeedSessionState.Blocking)
  ├─ "그만 볼래요" 선택 → 30초 타이머 → 의도 확인 → HOME 강제 종료
  ├─ "더 볼래요" 선택 → 30초 타이머 → 의도 확인 → 누적 리셋 + N분 추가 허용
  └─ 백버튼 → 그만 볼래요와 동일 처리
```

### 5.3 차단 후 액션

| 사용자 선택 | 액션 | 누적 상태 |
|---|---|---|
| 그만 볼래요 | 30초 타이머 후 `GLOBAL_ACTION_HOME` (강제 종료) | 세션 리셋 |
| 더 볼래요 | 30초 타이머 후 오버레이 해제 | 누적 리셋 + 5초 쿨다운 |
| 백버튼 / 화면 꺼짐 | 그만 볼래요와 동일 | 세션 리셋 |

### 5.4 30초 타이머 정책

쇼츠 차단과 동일한 **마찰(friction) 시간** 적용:
- 사용자가 옵션을 선택해도 30초 카운트다운
- "정말 더 봐야 할까요?" 메시지 표시 (반성 유도)
- 카운트다운 동안 백그라운드 진입 시 즉시 강제 종료

### 5.5 쿨다운

- 차단 오버레이 dismiss 후 **5초 동안 재차단 금지** (반복 트리거 방지)
- "더 볼래요" 선택 시 **누적 거리/시간 모두 0으로 리셋** → 다시 임계값 도달까지 자유 사용

### 5.6 누적 라이프사이클 정책 (회피 방지)

피드 차단의 핵심 우회 경로(잠시 다른 화면으로 갔다 돌아오기)를 막기 위해 앱별 차등 정책을 적용한다.

#### 공통 원칙

- **활성 시간만 카운트**: 화면 ON + 대상 앱이 포그라운드인 시간만 누적 시간에 가산
- **다른 앱 이동 시 일시정지**: 누적 값(거리/시간) 보존, 시간 카운트 멈춤
- **자정(00:00) 자동 리셋**: 매일 0시에 모든 누적 0으로 초기화 (un:short 일일제한 패턴과 동일)
- **명시적 리셋**: "더 볼래요" / "그만 볼래요" 선택 시 즉시 누적 리셋

#### Instagram 정책 (같은 세션 유지)

| 동작 | 처리 |
|---|---|
| 홈피드 → 릴스/탐색/DM/프로필 (앱 내 이동) | 거리 카운트 계속, 시간 카운트 계속 (포그라운드+화면 ON) |
| Instagram → 다른 앱 또는 홈 | 누적 일시정지 (값 보존) |
| 다른 앱 → Instagram 복귀 | 즉시 이어감 (grace 무제한) |
| 화면 OFF | 시간 카운트 일시정지 |
| 화면 ON 후 Instagram 복귀 | 즉시 이어감 |

> **거리 누적 범위**: Instagram 앱 내 모든 스크롤(피드/릴스/탐색) 거리 누적. 단, 차단 오버레이 트리거 판단은 "홈피드 진입 상태"에서만 수행.

#### YouTube 정책 (5분 Grace Period)

| 동작 | 처리 |
|---|---|
| 홈피드 → 영상 시청/검색/구독 등 (앱 내 이동) | 거리 카운트 일시정지, 시간 카운트 일시정지 (홈피드만 활성) |
| 홈피드 → 다른 앱 또는 홈 | 누적 일시정지, **5분 카운트다운 시작** |
| 5분 내 홈피드 복귀 | 누적 이어감 |
| 5분 초과 후 홈피드 복귀 | **누적 자동 리셋 후 새 세션 시작** |
| 화면 OFF | YouTube 정책상 다른 앱 이동과 동일 처리 (5분 카운트다운) |

> **시간/거리 누적 범위**: YouTube 홈피드 화면에 한정. 영상 시청은 의도적 사용으로 간주하여 누적에서 제외.

#### 정책 차이 근거

- **Instagram**: 피드 ↔ 릴스 ↔ DM 탭 전환이 잦고 모두 "Instagram 사용" 한 카테고리. 회피 가능성 높아 grace 무제한.
- **YouTube**: 홈피드와 영상 시청은 명확히 분리된 경험. 영상 1편 시청 후 복귀까지 5분 이상 걸리면 자연스러운 새 세션.

---

## 6. 사용자 시나리오

### 6.1 시나리오 A: 임계값 도달 후 그만보기

```
1. 사용자가 Instagram 실행, 홈피드 진입
2. 스크롤 시작 (3~5분 경과)
3. 누적 거리 14,775px 또는 3분 도달
4. un:short 차단 오버레이 표시
   - "잠깐만요! 🤚"
   - 30초 타이머 시작
   - "정말 지금 더 봐야 할까요?" 메시지
5. 사용자가 "그만 볼래요" 클릭
6. 카운트다운 후 자동으로 홈 화면 이동
7. 다음 진입 시 새로운 세션으로 카운팅 재시작
```

### 6.2 시나리오 B: 임계값 도달 후 계속 보기

```
1. 누적 임계값 도달 → 차단 오버레이
2. 사용자가 "더 볼래요" 클릭
3. 30초 타이머 후 오버레이 해제
4. 누적 거리/시간 리셋
5. 추가로 임계값까지 자유 스크롤 가능
6. 다시 임계값 도달 → 오버레이 재표시
```

### 6.3 시나리오 C: 베타 토글 OFF 후 사용

```
1. 사용자가 설정 → 고급 기능 → "피드 차단 (베타)" OFF
2. AccessibilityService는 동작하지만 피드 감지/누적 비활성화
3. Instagram 홈피드 자유 사용 (쇼츠는 기존대로 차단)
```

---

## 7. 기능 요구사항 (Functional Requirements)

### 7.1 핵심 기능

| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-1 | Instagram 홈피드 화면을 정확히 식별한다 (릴스/DM/프로필 제외) | P0 |
| FR-2 | 피드 내 누적 스크롤 거리를 측정한다 (`scrollDeltaY` 또는 `scrollY` 차분) | P0 |
| FR-3 | 피드 진입 후 경과 시간을 측정한다 | P0 |
| FR-4 | 임계값 도달 시 차단 오버레이를 자동 표시한다 | P0 |
| FR-5 | 30초 타이머 후 사용자 선택 액션을 수행한다 | P0 |
| FR-6 | "그만보기" 시 GLOBAL_ACTION_HOME으로 강제 종료한다 | P0 |
| FR-7 | "더보기" 시 누적 리셋 후 추가 사용을 허용한다 | P0 |
| FR-8 | 5초 쿨다운으로 반복 트리거를 방지한다 | P0 |
| FR-9 | 활성 시간만 누적한다 (포그라운드 + 화면 ON 조건) | P0 |
| FR-10 | Instagram 정책: 같은 세션 유지 (앱 이탈 시 일시정지, 복귀 시 이어감, grace 무제한) | P0 |
| FR-11 | YouTube 정책: 다른 앱 5분 초과 이탈 시 누적 자동 리셋 | P0 |
| FR-12 | 자정(00:00) 시 모든 누적을 자동 리셋한다 | P0 |
| FR-13 | Instagram 거리 누적은 앱 내 모든 스크롤(피드/릴스/탐색)을 합산하나, 트리거 판단은 홈피드 진입 시에만 수행한다 | P0 |
| FR-14 | YouTube 시간/거리 누적은 홈피드 화면에 한정한다 (영상 시청 등 제외) | P0 |
| FR-15 | 베타 토글 OFF 시 FeedBlockService를 시스템에서 disable한다 | P0 |
| FR-16 | 베타 토글 ON 시 별도 접근성 권한 안내 다이얼로그를 노출한다 | P0 |
| FR-16a | 권한이 이미 활성화된 상태에서 토글 ON 시 권한 안내 화면을 스킵한다 | P0 |
| FR-16b | 시스템에서 권한이 임의 OFF되면 onResume에서 감지하여 토글을 자동 동기화한다 | P0 |
| FR-16c | 설정 진입 후 미활성화 복귀 시 토글을 OFF로 되돌린다 | P0 |
| FR-16d | 앱 데이터 초기화 후 권한 잔존 시 복원 안내 다이얼로그를 노출한다 | P1 |
| FR-17 | 사용자가 임계값(거리/시간)을 설정 화면에서 조정할 수 있다 | P1 |
| FR-18 | YouTube 홈피드 지원 (Phase 2) | P1 |

### 7.2 비기능 요구사항

| ID | 요구사항 |
|---|---|
| NFR-1 | AccessibilityEvent 처리 시 ANR 발생 금지 (워커 스레드 처리) |
| NFR-2 | 배터리 영향 최소화 (PoC 기준 추가 영향 미미 확인 필요) |
| NFR-3 | 피드 외 화면(릴스/DM/프로필)에서 false positive 0% 목표 |
| NFR-4 | Instagram 앱 업데이트로 ViewID 변경 시 24시간 내 hotfix 가능한 구조 (Remote Config) |
| NFR-5 | 기존 ShortsBlockService와 코드/상태 분리 (피드 차단 결함이 쇼츠 차단에 영향 X) |
| NFR-6 | FeedBlockService와 ShortsBlockService가 같은 패키지 이벤트를 동시 수신해도 각자 책임 영역만 처리 |

---

## 8. 기술 아키텍처

### 8.1 모듈 구조 (별도 서비스 격리)

기존 `ShortsBlockService`는 손대지 않고, **신규 `FeedBlockService`를 독립 패키지로 격리**한다. 베타 기능 결함이 핵심 쇼츠 차단에 영향을 주지 않도록 코드/상태/매니페스트 모두 분리.

```
app/src/main/java/com/muuu/unshort/
├── ShortsBlockService.kt                    # 기존, 손대지 않음
├── service/blocking/                        # 기존, 손대지 않음
│   └── ... (쇼츠 차단 코드)
│
└── feedblock/                               # 신규 격리 패키지
    ├── FeedBlockService.kt                  # 별도 AccessibilityService
    ├── detection/
    │   ├── FeedDetectionEngine.kt
    │   ├── FeedTargetRegistry.kt
    │   └── ScrollAccumulator.kt
    ├── lifecycle/
    │   ├── FeedSessionManager.kt            # 라이프사이클 정책 관리
    │   └── DailyResetReceiver.kt            # 자정 알람
    ├── overlay/
    │   ├── FeedBlockOverlayActivity.kt
    │   └── FeedBlockTimerActivity.kt        # 30초 타이머
    ├── prefs/
    │   └── FeedBlockPreferences.kt          # 피드 차단 전용 설정
    └── settings/
        └── AdvancedSettingsActivity.kt      # 고급 기능 설정 화면
```

### 8.2 매니페스트 등록 (별도 서비스)

```xml
<!-- 기존 -->
<service
    android:name=".ShortsBlockService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

<!-- 신규 (베타) -->
<service
    android:name=".feedblock.FeedBlockService"
    android:exported="false"
    android:label="@string/feed_block_service_label"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/feed_block_service_config" />
</service>
```

각 서비스는 독립된 `service_config.xml`을 가지며 `packageNames` 속성으로 책임 영역을 구분한다.

| Service | packageNames |
|---|---|
| ShortsBlockService | youtube, instagram, facebook, tiktok, naver |
| FeedBlockService | instagram, youtube (Phase 2) |

> Instagram은 양쪽에 등록되지만 각 서비스가 자기 책임 영역(쇼츠 vs 피드)만 처리한다.

### 8.3 데이터 모델 (Feed 전용, ShortsBlockConfig와 분리)

```kotlin
data class FeedTarget(
    val packageName: String,
    val displayName: String,
    val feedContainerViewIds: List<String>,
    val feedItemSignatureViewIds: List<String>,
    val homeTabViewId: String,
    val excludeViewIds: List<String>,
    val lifecyclePolicy: LifecyclePolicy
)

sealed interface LifecyclePolicy {
    /**
     * Instagram 정책: 다른 앱 이탈 시 누적 일시정지, 복귀 시 이어감 (grace 무제한)
     */
    data object InstagramSameSession : LifecyclePolicy

    /**
     * YouTube 정책: 다른 앱 5분 초과 이탈 시 누적 자동 리셋
     */
    data class YouTubeGracePeriod(val gracePeriodMs: Long = 5 * 60 * 1000L) : LifecyclePolicy
}
```

### 8.3 식별자 (PoC 검증 완료)

#### Instagram (Phase 1)
- 피드 컨테이너:
  - `com.instagram.android:id/swipeable_nav_view_pager_inner_recycler_view`
  - `com.instagram.android:id/sticky_header_list`
  - `com.instagram.android:id/refreshable_container`
- 피드 아이템 시그니처:
  - `row_feed_view_group_social_ufi_buttons`
  - `row_feed_photo_imageview`
  - `row_feed_profile_header`
- 홈탭: `com.instagram.android:id/feed_tab` + isSelected
- 제외: `clips_viewer_view_pager`, `reels_tray_container`

#### YouTube 홈피드 (Phase 2 - 추후 PoC 필요)
- 식별자 추후 검증 (Phase 2 진입 시 별도 PoC)

### 8.4 상태 머신 (FeedBlockService 독립)

기존 `BlockingStage` (ShortsBlockService 영역)는 손대지 않고, FeedBlockService 전용 상태 머신을 별도 정의한다.

```kotlin
// feedblock/lifecycle/FeedSessionState.kt
sealed interface FeedSessionState {
    data object Idle : FeedSessionState
    data class Active(
        val packageName: String,
        val sessionStartTime: Long,
        val accumulatedTimeMs: Long,
        val accumulatedDistancePx: Int,
        val lastPauseTime: Long? = null   // 다른 앱 이탈 시점
    ) : FeedSessionState
    data class Paused(
        val packageName: String,
        val accumulatedTimeMs: Long,
        val accumulatedDistancePx: Int,
        val pausedAt: Long
    ) : FeedSessionState
    data object Blocking : FeedSessionState     // 임계값 도달, 오버레이 표시 중
    data object CoolingDown : FeedSessionState  // "더 볼래요" 후 5초 쿨다운
}
```

상태 전이는 `FeedSessionManager`가 라이프사이클 정책(§5.6)에 따라 단일 책임으로 관리.

### 8.5 Remote Config (운영 안정성)

Instagram/YouTube 앱 업데이트로 ViewID 변경 시 신속 대응을 위해 Firebase Remote Config로 다음을 노출:
- `feed_block_instagram_container_ids`
- `feed_block_instagram_signature_ids`
- `feed_block_instagram_exclude_ids`
- `feed_block_default_scroll_multiplier`
- `feed_block_default_time_threshold_ms`

### 8.6 미디어 제어

피드는 자동재생 영상이 음소거 상태라 **`controlsMedia = false`** 기본값. 별도 미디어 정지 로직 불필요.

### 8.7 ExitAction

피드 차단 시 `ExitAction.HOME` 사용 (홈피드는 앱 루트라 백버튼이 앱 종료가 됨).

---

## 9. UX/UI 사양

### 9.1 차단 오버레이 (un:short 디자인 시스템 적용)

```
┌─────────────────────────────────┐
│                                 │
│   잠깐만요! 🤚                    │
│                                 │
│   ┌─────────────────────┐       │
│   │      00:30          │       │
│   │   초 후 자동 종료      │       │
│   └─────────────────────┘       │
│                                 │
│   정말 지금 더 봐야 할까요?         │
│                                 │
│   ┌─────────────────────┐       │
│   │  그만 볼래요          │       │
│   └─────────────────────┘       │
│   ┌─────────────────────┐       │
│   │  계속 볼래요 (30초 후)  │       │
│   └─────────────────────┘       │
│                                 │
└─────────────────────────────────┘
```

**오버레이 표시 원칙**:
- 사용 시간/스크롤 거리 등 정량 통계는 **표시하지 않음** (사용자에게 죄책감 유발 방지, 의식 전환에 집중)
- 쇼츠 차단 오버레이와 동일한 톤 ("잠깐만요! 🤚")으로 일관성 유지
- 기존 un:short 디자인 시스템(`primary_dark`, `accent_purple`, 88sp 타이머) 그대로 적용

### 9.2 디자인 토큰

기존 un:short 디자인 시스템 (`primary_dark`, `accent_purple`, success/danger) 그대로 사용.

### 9.3 설정 화면 (고급 기능 토글)

```
설정
├── 차단 대상 앱 (기존)
├── 일일 제한 (기존)
├── 알림 (기존)
└── 고급 기능 (신규 섹션)
    └── 피드 차단 (베타) ──── [Toggle]
        ├── 권한 상태: 활성/비활성  ──── [설정 열기]
        ├── 누적 스크롤 임계값  [3배 ───●─── 10배] (기본 5배)
        ├── 누적 시간 임계값   [1분 ──●──── 10분] (기본 3분)
        └── 차단 대상 앱
            ├── ☑ Instagram 홈피드
            └── ☐ YouTube 홈피드 (Phase 2)
```

### 9.4 베타 토글 활성화 흐름 (별도 권한 안내)

피드 차단은 별도 AccessibilityService이므로 **추가 접근성 권한이 필요**하다.

```
1. 토글 ON
   ↓
2. 베타 안내 다이얼로그
   "피드 차단은 베타 기능입니다.
    쇼츠 차단과 달리 피드는 임계값 도달 시 알림을 표시합니다.
    별도 접근성 권한이 필요해요. 언제든 OFF 가능합니다.
    [계속] [취소]"
   ↓
3. 권한 안내 화면
   "다음 단계에서 'FeedBlock (un:short)' 항목을 활성화해주세요"
   [설정 열기]
   ↓
4. 시스템 접근성 설정 진입
   ↓
5. 사용자가 FeedBlockService 활성화
   ↓
6. un:short 복귀 → 권한 활성 확인 → 토글 최종 ON
```

토글 OFF 시:
- `PackageManager.setComponentEnabledSetting`으로 FeedBlockService를 `COMPONENT_ENABLED_STATE_DISABLED` 처리
- 시스템 접근성 목록에서 항목이 사라져 사용자 혼란 최소화

### 9.4.1 권한 상태 동기화 엣지 케이스

베타 토글과 시스템 접근성 권한이 불일치하는 케이스를 처리한다.

| 케이스 | 상황 | 처리 |
|---|---|---|
| **이미 권한 있는 상태에서 토글 ON** | 사용자가 토글을 껐다 켰다 반복 | 베타 안내 다이얼로그만 표시 → 권한 안내 화면 스킵 → 즉시 토글 ON |
| **시스템에서 권한 임의 OFF** | 토글 ON이지만 시스템 권한이 비활성 | `MainActivity.onResume`에서 주기 확인 → 토글 자동 OFF 동기화 + 알림: "피드 차단이 시스템 설정에서 비활성화되었어요" |
| **설정 진입 후 미활성화 복귀** | 사용자가 권한 안내 화면 → 시스템 설정 → 활성화 없이 뒤로가기 | onResume에서 권한 재확인 → 토글 OFF로 되돌림 + 토스트: "활성화가 완료되지 않았어요" |
| **토글 OFF (베타 비활성화)** | 사용자가 베타 기능 종료 | (1) FeedSessionManager 리셋 (2) 오버레이 dismiss (3) `setComponentEnabledSetting` 으로 서비스 disable (4) SharedPreferences OFF |
| **앱 데이터 초기화 후 권한 잔존** | 앱 재설치 / 데이터 클리어 후 시스템 권한만 ON 상태 | 첫 실행 시 권한 vs 토글 상태 동기화 다이얼로그: "이전에 사용 중이던 피드 차단을 다시 활성화할까요?" |
| **차단 오버레이 표시 중 토글 OFF** | 임계 도달 후 오버레이 활성 상태에서 사용자가 베타 OFF | 즉시 오버레이 finish + 누적 리셋 + 서비스 disable (FR-15 참조) |

#### 권한 상태 감지 구현 패턴

```kotlin
// MainActivity / AdvancedSettingsActivity
override fun onResume() {
    super.onResume()
    syncFeedBlockToggleWithSystemPermission()
}

private fun syncFeedBlockToggleWithSystemPermission() {
    val toggleEnabled = prefs.isFeedBlockEnabled
    val systemEnabled = isFeedBlockServiceEnabled()

    when {
        toggleEnabled && !systemEnabled -> {
            prefs.isFeedBlockEnabled = false
            showToast("피드 차단이 시스템에서 비활성화되었어요")
            updateUI()
        }
        !toggleEnabled && systemEnabled -> {
            // 데이터 초기화 후 잔존 권한 케이스
            showRestoreDialog()
        }
    }
}

private fun isFeedBlockServiceEnabled(): Boolean {
    val enabled = Settings.Secure.getString(
        contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val expected = "$packageName/${FeedBlockService::class.java.name}"
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}
```

---

## 10. Phase별 로드맵

### Phase 1: Instagram 홈피드 (v2.0 Beta)

**기간**: 2주

| 작업 | 산출물 |
|---|---|
| 1. `feedblock/` 격리 패키지 생성 + PoC 코드 이식 | FeedBlockService, FeedDetectionEngine, ScrollAccumulator |
| 2. FeedBlockService 매니페스트 등록 + service_config.xml | 별도 AccessibilityService |
| 3. FeedSessionManager 구현 (라이프사이클 정책) | InstagramSameSession 정책 |
| 4. DailyResetReceiver 구현 (자정 알람) | AlarmManager 기반 |
| 5. FeedBlockOverlayActivity + FeedBlockTimerActivity (30초 타이머) | un:short 디자인 시스템 적용 |
| 6. AdvancedSettingsActivity 구현 (베타 토글 + 임계값 슬라이더) | 고급 기능 설정 화면 |
| 7. 베타 토글 활성화 흐름 (권한 안내 + setComponentEnabledSetting) | 온보딩 다이얼로그 |
| 8. Firebase Remote Config 연동 (식별자/임계값 원격 제어) | 운영 hotfix 체계 |
| 9. 회귀 테스트 (쇼츠 차단 영향 검증) | 기존 기능 무결성 확인 |
| 10. Beta 채널 출시 (CLOSED Beta) | Internal testing |

### Phase 2: YouTube 홈피드 (Phase 1 안정화 후)

**기간**: 1.5주 (Phase 1 패턴 재사용 가능)

YouTube 홈피드는 `LifecyclePolicy.YouTubeGracePeriod(5분)` 정책 적용.

| 작업 | 산출물 |
|---|---|
| 1. YouTube 홈피드 PoC | feedblock-poc 재활용 |
| 2. YouTube 식별자 검증 | uiautomator dump 분석 |
| 3. AppBlockingRegistry에 YouTube FeedConfig 추가 | 식별자 등록 |
| 4. 쇼츠 감지와의 우선순위 조정 (쇼츠 우선) | 분기 로직 |
| 5. Beta 출시 | Public Beta |

### Phase 3 (Out of Scope, 후속 검토)

- Threads, X(Twitter), Facebook, 네이버 등 추가 앱
- 피드 사용 통계 화면
- 쇼츠/피드 일일제한 통합

---

## 11. 리스크 및 대응

| 리스크 | 영향 | 발생 가능성 | 대응 방안 |
|---|---|---|---|
| Instagram 앱 업데이트로 ViewID 변경 | 피드 감지 실패 | 중 | Firebase Remote Config로 식별자 원격 제어 + 24h hotfix 체계 |
| 임계값이 너무 낮아 사용자 짜증 | 베타 비활성화율 증가 | 중 | 4주 측정 후 기본값 재조정, 사용자 커스터마이징 노출 |
| 임계값이 너무 높아 차단 효과 미미 | 베타 가치 미체감 | 중 | A/B 테스트로 기본값 최적화 |
| 정보 탐색 중인 사용자 불필요 차단 | 사용성 저하 | 중 | "더 볼래요" 옵션 + 누적 리셋으로 완화 |
| Instagram 정책의 grace 무제한으로 누적 과도 축적 | 다음 진입 즉시 차단 | 중 | 자정 자동 리셋 + "더 볼래요" 시 즉시 리셋으로 완화. 베타 측정 후 grace 정책 재검토 |
| 두 접근성 권한 활성화 부담으로 베타 활성화율 저조 | 가설 검증 데이터 부족 | 중 | 온보딩 다이얼로그로 가치 명확화 + 권한 안내 흐름 단순화 |
| AccessibilityEvent 폴링 과다로 배터리 소모 | 사용자 이탈 | 저 | PoC에서 워커 스레드 + 이벤트 기반 처리 검증 완료. 두 서비스 동시 운영 영향 측정 필요 |
| 릴스/DM 화면에서 false positive | 사용자 신뢰 하락 | 저 | excludeViewIds로 명시적 제외, QA 강화 |
| 차단 오버레이가 시스템 알림(전화 등)에 가려짐 | 차단 무효화 | 저 | 시스템 우선순위 화면은 정상 동작으로 인정, 종료 후 세션 리셋 |
| 자정 리셋 시점에 사용자가 사용 중 | 임계 도달 직후 리셋되어 즉시 차단 해제 | 저 | DailyResetReceiver는 단순 리셋만 수행. 다음 임계 도달까지 정상 동작 |

---

## 12. Open Questions

1. **베타 노출 범위**: 전체 사용자에게 토글 노출할지, 일부 사용자에게만 (Remote Config 기반 점진 노출)?
   - 추천: 점진 노출 (1% → 10% → 50% → 100%)

2. **임계값 단위**: 사용자에게 "화면 높이 × N배"가 직관적인지, "스크롤 횟수 N회"가 직관적인지?
   - 추천: 베타 사용자 인터뷰 후 결정. 초기는 "체감 시간"으로 노출 ("약 3분 정도 본 후")

3. **차단 횟수 제한**: 한 세션에서 "더 볼래요"를 무제한 선택 가능한지, N회 이후 강제 종료할지?
   - 추천: Phase 1은 무제한 → 4주 측정 후 결정

4. **YouTube 쇼츠와의 충돌**: YouTube 앱에서 쇼츠 진입 시 쇼츠 차단이 우선 발동, 홈피드에서는 피드 차단이 발동하는 분기 로직 검증 필요
   - 대응: Phase 2 PoC에서 검증

5. **다국어**: 베타 단계에서 영어 외 언어 모두 지원할지?
   - 추천: 한국어/영어 우선, 나머지는 기존 다국어 시스템 자동 적용

6. **YouTube grace period 5분 적정성**: 영상 1편 시청 시간이 5분을 넘으면 누적 리셋되어 회피 경로가 됨
   - 대응: Phase 2 진입 시 실제 시청 패턴 측정 후 3~10분 사이 조정

7. **누적 거리/시간 영구 보존 우려**: Instagram 사용자가 하루 종일 백그라운드로 두면 계속 누적
   - 대응: 자정 자동 리셋 + 활성 시간(포그라운드+화면 ON)만 카운트로 완화

8. **베타 토글 OFF 시 진행 중 세션 처리**: 차단 오버레이 표시 중 토글이 OFF되면?
   - 추천: 즉시 오버레이 dismiss + 누적 리셋 + 서비스 disable

---

## 13. 참고 자료

- PoC 검증 기록: 2026-04-28 Instagram 홈피드 누적 14,775px 도달 후 오버레이 자동 호출 확인
- 기존 쇼츠 차단 구현: `app/src/main/java/com/muuu/unshort/ShortsBlockService.kt`
- AppBlockingRegistry: `app/src/main/java/com/muuu/unshort/service/blocking/AppBlockingRegistry.kt`
- un:short 디자인 시스템: `CLAUDE.md`, `DESIGN_SYSTEM.md`

---

## 14. 변경 이력

| 일자 | 버전 | 변경 사항 | 작성자 |
|---|---|---|---|
| 2026-04-28 | v1.0 | 초안 작성 (PoC 검증 후) | hsikkk |
| 2026-05-01 | v1.1 | 차단 오버레이에서 사용 통계 표시 제거 (의식 전환 집중) | hsikkk |
| 2026-05-03 | v1.2 | 별도 FeedBlockService로 격리 + 누적 라이프사이클 정책(Instagram 같은 세션 / YouTube 5분 grace) + 자정 자동 리셋 추가 | hsikkk |
| 2026-05-03 | v1.3 | 9.4.1 권한 상태 동기화 엣지 케이스 6종 추가 (이미 권한 있음/임의 OFF/미활성 복귀/토글 OFF/데이터 초기화 잔존/오버레이 중 OFF) | hsikkk |
