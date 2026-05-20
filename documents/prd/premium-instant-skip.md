# PRD: 프리미엄 — 차단 화면 즉시 닫기/즉시 보기

> **Status:** Draft
> **Owner:** Product
> **Last Updated:** 2026-05-17
> **Related:** [feed-block.md](./feed-block.md), [daily-unblock-quota.md](./daily-unblock-quota.md)

---

## 1. 배경 (Why)

현재 차단 오버레이는 사용자가 "충동적 소비"를 막기 위해 **버튼 활성화에 인위적 딜레이(카운트다운)** 를 둔다.

- 쇼츠 차단 INITIAL 화면 — `"그만 볼래요"`, `"바로 보기"` 두 버튼이 **2초** 비활성화
- 피드 차단 오버레이 — `"계속 볼래요(OK)"` 버튼이 **5초** 비활성화

이 마찰(friction)은 무료 사용자에게는 핵심 가치(의도적 소비)지만, **프리미엄 결제 사용자에게는 "내가 돈 내고 쓰는데도 매번 기다려야 한다"는 불편**으로 작용한다. 프리미엄 가치 제안을 강화하고 결제 전환의 미세 동기를 추가하기 위해 **프리미엄 = 딜레이 0초** 옵션을 제공한다.

> 사용자 원문: "현재 차단 화면 그냥 닫기/바로 보기에 3초 딜레이가 있는데. 프리미엄에서는 이거 3초 없이 바로 지원을 추가하고 싶어."
> (실제 코드 상 딜레이는 쇼츠 2초 / 피드 5초이며 PRD에서는 정확한 수치로 기술한다.)

---

## 2. 목표 / 비목표

### Goals
- 프리미엄 사용자에게 **차단 오버레이의 모든 카운트다운 딜레이를 0초로 제거**
- 무료 사용자의 기존 마찰(2초/5초) 경험은 **변경 없이 유지**
- 프리미엄 결제 페이지의 "프리미엄 혜택" 목록에 본 기능 노출

### Non-Goals
- 메인 액션(`"한번 더 생각해보기"` = 타이머 시작) 자체를 건너뛰는 기능 → 별도 논의
- "폰 뒤집기" 등 다른 마찰 메커니즘 제거 → 본 PRD 범위 외
- 카운트다운 초 수를 사용자가 임의 설정하는 기능 → 본 PRD 범위 외

---

## 3. 사용자 스토리

| ID | As a | I want | So that |
|----|------|--------|---------|
| US-1 | **프리미엄 사용자** | 차단 화면에서 닫기/바로 보기를 즉시 누를 수 있다 | 정말 필요할 때 기다리지 않고 빠르게 빠져나간다 |
| US-2 | **무료 사용자** | 기존처럼 2~5초 카운트다운 후 누를 수 있다 | 충동을 한 번 더 인지하고 결정할 수 있다 |
| US-3 | **프리미엄 검토자** | 결제 페이지에서 "딜레이 없음" 혜택을 확인한다 | 가치 인식 후 결제를 결정한다 |

---

## 4. 현재 동작 (As-Is)

### 4.1 쇼츠 차단 — `ShortsBlockOverlayActivity`

| 버튼 | 위치 | 파일:라인 | 현재 딜레이 |
|------|------|-----------|-------------|
| `"그만 볼래요"` (skip) | INITIAL 상태 | `app/src/main/java/com/muuu/unshort/ui/activity/ShortsBlockOverlayActivity.kt:242-265` | **2초** (`skipButtonCountdown = 2`) |
| `"바로 보기"` (instant unblock) | INITIAL 상태 | 같은 파일 `:432-449` | **2초** (`instantUnblockCountdown = 2`) |
| `"안볼래요" / "볼래요"` | CONFIRMATION 상태 (타이머 완료 후) | `:283-318` | 딜레이 없음 |

문자열 리소스:
- `R.string.block_button_close_countdown` — `"Close (available in %1$ds)"`
- `R.string.instant_unblock_countdown` — `"Watch now (available in %1$ds)"`

### 4.2 피드 차단 — `FeedBlockOverlayActivity`

| 버튼 | 파일:라인 | 현재 딜레이 |
|------|-----------|-------------|
| `"계속 볼래요(OK)"` (continue) | `app/src/main/java/com/muuu/unshort/feedblock/overlay/FeedBlockOverlayActivity.kt:48-65, 191` | **5초** (`CONTINUE_DELAY_SECONDS = 5`) |
| `"그만 볼래요(stop)"` | 같은 파일 `:142-153` | 딜레이 없음 |

문자열 리소스:
- `R.string.feed_block_overlay_continue_countdown` — `"Continue (%1$d)"`

---

## 5. 변경 후 동작 (To-Be)

### 5.1 핵심 규칙

```
딜레이(초) = if (PremiumManager.isPremium()) 0 else <기존 값>
```

| 상황 | 무료 | 프리미엄 |
|------|------|----------|
| 쇼츠 INITIAL `"그만 볼래요"` | 2초 카운트다운 | **즉시 활성화** |
| 쇼츠 INITIAL `"바로 보기"` | 2초 카운트다운 | **즉시 활성화** |
| 쇼츠 CONFIRMATION 버튼들 | 즉시(현행) | 즉시(현행) |
| 피드 차단 `"계속 볼래요"` | 5초 카운트다운 | **즉시 활성화** |
| 피드 차단 `"그만 볼래요"` | 즉시(현행) | 즉시(현행) |

### 5.2 UI 동작

- **프리미엄**: 버튼이 처음부터 `isEnabled = true`, `alpha = 1.0f`, 라벨은 정상 텍스트(`Close`, `Watch now`, `Continue`). 카운트다운 텍스트는 **표기하지 않음**.
- **무료**: 기존 그대로. `0.5f` 알파 → 카운트다운 표기 → 0초 시 정상 텍스트.

### 5.3 결제 페이지 노출

`PremiumUpgradeActivity`(또는 동등 화면)의 혜택 목록에 한 줄 추가:

> "차단 화면 즉시 닫기 — 카운트다운 없이 바로 빠져나오기"

(정확한 카피는 마케팅 검토 후 확정)

---

## 6. 기술 설계

### 6.1 영향 범위 (최소 diff)

**파일 1: `ShortsBlockOverlayActivity.kt`**
- `startSkipButtonCountdown()` (`:242`) 진입부에서 `PremiumManager.isPremium()` 체크 → true면 카운트다운 스킵하고 버튼을 즉시 활성화 상태로 세팅 후 return.
- `startInstantUnblockCountdown()` (`:432`) 동일 패턴.

**파일 2: `FeedBlockOverlayActivity.kt`**
- `renderUi()` (`:122-158`) 내 카운트다운 시작부에서 `PremiumManager.isPremium()` 체크 → true면 `continueButton.isEnabled = true`, `alpha = 1f`, 텍스트 = `R.string.feed_block_overlay_continue`, `countdownHandler.post(...)` **호출하지 않음**.

### 6.2 헬퍼 함수 (권장)

각 Activity에 직접 분기를 넣지 말고, 단일 진입점을 둔다.

```kotlin
// 예: OverlayCountdownPolicy.kt (core/common 또는 service/blocking 하위)
object OverlayCountdownPolicy {
    fun resolveDelaySeconds(default: Int): Int =
        if (PremiumManager.isPremium()) 0 else default
}
```

각 카운트다운 함수는 `OverlayCountdownPolicy.resolveDelaySeconds(default = 2)`로 시작 값을 받고, **0이면 카운트다운 루프를 시작하지 않고 즉시 활성화 상태로 그린다**.

### 6.3 프리미엄 상태 캐싱

`PremiumManager.isPremium()` (`app/src/main/java/com/muuu/unshort/premium/PremiumManager.kt:101`)은 `isPremiumCache` 메모리 값을 반환하므로 **오버레이 onCreate 시점 1회 호출로 충분**. 별도 비동기 처리 불필요.

### 6.4 프리미엄 상태 변경 중 화면이 떠있는 경우 (Edge)

오버레이가 **수 초 이상 떠있는 동안 프리미엄이 활성화/해지될 가능성은 사실상 0**. 다만 안전을 위해:
- `PremiumManager.addPremiumChangeListener` 활용은 **하지 않음** (오버오버엔지니어링).
- 다음 차단 트리거에서 자연스럽게 정책이 재평가된다.

---

## 7. 엣지 케이스

| # | 케이스 | 처리 |
|---|--------|------|
| E1 | 프리미엄 활성 + 첫 진입 | 카운트다운 텍스트 없이 즉시 정상 라벨 노출 |
| E2 | 프리미엄 해지 직후 첫 차단 | `isPremiumCache` 갱신 후 다음 진입부터 무료 정책 |
| E3 | 타이머 완료 후 INITIAL로 돌아옴 (`onResume` `:632-635`) | INITIAL 재진입 시에도 프리미엄이면 즉시 활성화 |
| E4 | 프로모 코드로 Lifetime Premium 활성화 | `PremiumManager.activatePromoCode` 가 캐시 갱신 → 다음 차단부터 적용 |
| E5 | 디버그 빌드(`DummyPremiumProvider`) | 디버그 토글에 따라 정책 적용 — QA 시 활용 가능 |
| E6 | `setupBottomActionUi()`의 `isFromScroll = true` 분기 | `instantUnblockButton`이 GONE 처리되는 기존 분기는 **유지**. 프리미엄이라도 스크롤 진입이면 노출하지 않는다 (별도 논의 전까지 현행 유지) |

---

## 8. 분석 (Analytics)

### 8.1 신규 이벤트

| 이벤트 | When | Params |
|--------|------|--------|
| `OVERLAY_INSTANT_SKIP_USED` | 프리미엄이 INITIAL `"그만 볼래요"`를 0초 딜레이로 클릭 | `overlay_type` = `shorts` \| `feed`, `source_package` |

### 8.2 기존 이벤트 보강

`OVERLAY_BUTTON_SKIP`, `INSTANT_UNBLOCK_CLICKED`에 `is_premium: Boolean` 파라미터 추가하여 무료/프리미엄 행동 비교 가능하게 한다.

### 8.3 측정 가설

- 프리미엄 사용자의 **차단 화면 평균 체류 시간 ↓**
- "바로 보기/그만 보기" 클릭률은 프리미엄 ≥ 무료 (마찰 제거 효과)
- 결제 페이지 진입 → 결제 전환에는 본 기능 단독 영향은 작을 것 — 부가 가치 위주

---

## 9. 검증 시나리오 (QA)

### 9.1 기능 검증
1. **무료 회귀**: 무료 상태에서 쇼츠 진입 → 2초 카운트다운 정상 동작, 피드 진입 → 5초 카운트다운 정상 동작.
2. **프리미엄 즉시**: 프리미엄 활성화 후 쇼츠/피드 진입 → 모든 보조 버튼 즉시 활성화, 카운트다운 텍스트 미노출.
3. **상태 전환**: 무료 → 프리미엄 결제 직후 새 차단 트리거 → 즉시 활성화.
4. **CONFIRMATION 영향 없음**: 타이머 완료 후 화면의 `"안볼래요"`/`"볼래요"`는 기존과 동일.
5. **isFromScroll**: 스크롤 진입 시 `instantUnblockButton`이 GONE 유지 (프리미엄 여부 무관).

### 9.2 UI 시각 검증 (필수)
- `installDebug` 후 실 디바이스에서 두 화면 스크린샷 → Read 도구로 시각 확인
- 카운트다운 텍스트 잔여 표기 없음 확인

### 9.3 회귀
- 프리미엄 해지 시 카운트다운이 다시 적용되는지 (`onPremiumDowngrade` 흐름 후 차단 트리거)

---

## 10. 출시 계획

| 단계 | 내용 |
|------|------|
| 1. 구현 | 헬퍼 + 2개 Activity 분기 (예상 < 0.5d) |
| 2. QA | 위 시나리오 통과 |
| 3. 결제 페이지 카피 업데이트 | 마케팅 검토 후 추가 |
| 4. 분석 이벤트 추가 | 8절 이벤트 코드 반영 |
| 5. Changelog | "프리미엄 — 차단 화면 즉시 닫기 지원" |
| 6. 릴리스 | 다음 정기 버전에 포함 (별도 핫픽스 불필요) |

---

## 11. 확정 사항 / 잔여 이슈

### 확정 (2026-05-17)
- ✅ 프리미엄 = **0초**, 무료 = **현행 유지** (쇼츠 2초 / 피드 5초)
- ✅ 피드 차단 5초도 프리미엄에서는 0초로 동일 적용
- ✅ `isFromScroll` 케이스는 **현행 유지** (스크롤 진입 시 `instantUnblockButton` GONE — 프리미엄 여부 무관)

### 잔여
- [ ] 결제 페이지 혜택 카피 최종 문구 (마케팅 검토)

---

## 12. 참고 코드 위치 요약

| 항목 | 경로 |
|------|------|
| 쇼츠 오버레이 | `app/src/main/java/com/muuu/unshort/ui/activity/ShortsBlockOverlayActivity.kt` |
| 피드 오버레이 | `app/src/main/java/com/muuu/unshort/feedblock/overlay/FeedBlockOverlayActivity.kt` |
| 프리미엄 상태 | `app/src/main/java/com/muuu/unshort/premium/PremiumManager.kt` |
| 카운트다운 문자열 | `app/src/main/res/values/strings.xml` (`block_button_close_countdown`, `instant_unblock_countdown`, `feed_block_overlay_continue_countdown`) |
