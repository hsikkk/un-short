# PRD: 일일 즉시 해제 한도 + 광고 충전 시스템

> 이 문서는 기능 개발 당시의 요구사항과 결정 기록입니다. 현재 동작은 [현재 제품 스펙](../SPEC.md)을 기준으로 합니다.

| 항목 | 값 |
|---|---|
| **문서 버전** | v1.2 |
| **작성일** | 2026-05-06 |
| **최종 수정일** | 2026-08-02 |
| **작성자** | hsikkk |
| **상태** | Implemented |
| **릴리스 상태** | 현재 앱에 포함 |
| **선행 의존성** | Muuu Ad SDK 0.2.0 (`MuuuRewardedAdLoader` 지원 확인됨) |
| **통합 대상 기능** | 기존 프리미엄 "스크롤한 쇼츠만 차단" (`PREF_BLOCK_SCROLLED_ONLY`) |
| **저장소** | SharedPreferences (`PreferencesManager`) — DataStore 미사용 |
| **관련 구현** | `DailyUnblockQuotaManager`, `ShortsBlockOverlayActivity` |

---

## 1. 개요

### 1.1 배경

un:short는 현재 차단 오버레이(`ShortsBlockOverlayActivity`, `FeedBlockOverlayActivity`)에서 **30초 카운트다운 + 폰 뒤집기**의 의도적 마찰(friction)로 충동적 쇼츠/피드 시청을 억제하고 있다. 광고 수익화는 차단 화면 네이티브 광고 + 메인/타이머 배너로 구성되어 있다.

또한 기존에 프리미엄 전용 기능으로 **"스크롤한 쇼츠만 차단"** (`PREF_BLOCK_SCROLLED_ONLY`)이 존재한다. 이 기능은 **진입 방식**으로 사용자 의도를 추정하는 메커니즘이다:

| 진입 방식 | 사용자 의도 추정 | 기존 프리미엄 토글 ON 시 |
|---|---|---|
| **클릭 진입** (홈/검색에서 쇼츠 카드 탭) | 의도된 짧은 시청 | 차단하지 않음 |
| **스크롤 진입** (쇼츠 보기 중 다음으로 스와이프) | 무의식적 연속 시청 | 차단 |

추가 광고 수익화를 검토하는 과정에서 다음과 같은 트레이드오프가 식별되었다:

| 접근 | 광고 임프레션 | 사용자 경험 | 삭제 리스크 |
|---|---|---|---|
| 모든 차단 해제에 광고 강제 | 高 | 강압적 | 高 |
| 광고 위치 추가 (배너/인터스티셜 증설) | 中 | 거슬림 | 中 |
| **현행 유지** | 中 | 양호 | 低 |
| **일일 한도 + 광고 충전 (진입 방식 차등)** | **충동 사용자에 한해 高** | **일반 사용자 영향 없음** | **低** |

**핵심 통찰**: 진입 방식별 의도 추정 메커니즘은 일일 한도 시스템과 정확히 같은 철학을 공유한다 — **의도된 사용은 허용, 충동/우회는 마찰**. 두 시스템을 통합하면 별도 프리미엄 토글 없이도 기존 기능과 등가의 효과를 자연 발생시킬 수 있다.

### 1.2 문제 정의

**Problem Statement**: 차단 해제를 자주 시도하는 충동적 사용자(우회 시도형)와 정당한 사유로 가끔 해제하는 절제형 사용자를 동일하게 대우하면 광고 수익화 기회를 놓치거나, 반대로 강제 광고로 절제형 사용자를 잃는다.

### 1.3 핵심 가설

> **"클릭 진입(의도적)에 한해 하루 N회까지는 광고 없이 풀 수 있고, 그 이상은 광고 시청을 해야 풀 수 있다. 스크롤 진입(무의식적)은 한도와 무관하게 강한 마찰을 유지한다."** 이 정책은 (a) 절제형 사용자 경험을 해치지 않고 (b) 충동형 사용자에게는 자연스러운 마찰과 자기 인식을 제공하며 (c) 광고 임프레션을 충동 사용자 행동에 비례해 폭증시키고 (d) 프리미엄의 "한도 무제한"이 기존 "스크롤한 쇼츠만 차단" 기능과 효과적 등가성을 가진다.

광고를 "벌칙"이 아닌 **"한도 초과 시 추가 권리 획득 수단"**으로 재정의함으로써 사용자 자율감을 보존하고 매몰 비용 락인을 형성한다.

#### 등가성 증명

| 사용자 | 클릭 진입 처리 (통합 후) | 기존 "스크롤한 쇼츠만 차단" ON과의 등가성 |
|---|---|---|
| 프리미엄 (한도 무제한) | 무한 차단 해제 = **사실상 무제한 허용** | ✅ 등가 (마찰 30초만 추가 적용 — 차단 앱 정체성 유지) |
| 일반 (한도 ≥ 1) | 한도 차감 후 즉시 해제 | 기존 일반 동작과 유사하나 한도라는 자율 영역 확보 |
| 일반 (한도 = 0) | 광고 시청 후 +1 충전 → 해제 | 기존엔 무제한 30초 우회 가능 → 충동 사용자 마찰 강화 |

---

## 2. 목표 및 성공 지표

### 2.1 비즈니스 목표

- 충동 사용자에게 자연스러운 광고 임프레션 채널 확보 (광고 수익 +30~50% 목표)
- 절제 사용자 만족도 유지 → 삭제율 증가 0% 목표
- 프리미엄 전환 동기 강화 ("광고 없이 무제한 해제" 가치 명확화)

### 2.2 사용자 가치

- 일반 사용 패턴(하루 N회 이하)에는 광고 노출 거의 없음
- 한도 초과 시점에 자연스러운 자기 인식 트리거 발생 ("내가 너무 자주 풀고 있구나")
- 강제 광고가 아닌 **선택적 광고**로 자율감 유지

### 2.3 성공 지표 (출시 후 4주 측정)

| 지표 | 정의 | 목표 |
|---|---|---|
| **일일 한도 도달률** | 한도 도달 사용자 / DAU | 5~15% (낮으면 한도↑, 높으면 한도↓) |
| **한도 도달 후 광고 시청률** | 광고 시청 / 한도 도달 이벤트 | ≥ 30% |
| **광고 시청 후 해제 완료율** | 차단 해제 완료 / 광고 시청 | ≥ 70% |
| **신규 광고 임프레션** | 본 시스템 발생 광고 / 전체 광고 | ≥ 20% |
| **삭제율 변화** | 출시 전후 7일 삭제율 차이 | ≤ +0.5%p |
| **프리미엄 전환율 변화** | 출시 전후 30일 전환율 차이 | ≥ +20% |

### 2.4 Non-Goals

- 한도 자체를 사용자가 조정 가능하게 설정 노출 (초기엔 고정값)
- 게이미피케이션 요소 (보너스 크레딧, 등급, 배지 등)
- 메인 화면에 카운터/잔여 횟수 표시
- 광고 시청 시 마찰(30초 + 폰 뒤집기) 면제

---

## 3. 페르소나 / 타겟 사용자

### 3.1 Primary Persona A: "절제형 K"

- **프로필**: 30대 직장인, un:short 6개월차, 일일 차단 해제 1~3회
- **현재 상태**: 정당한 사유(업무 검색, 친구 추천 영상 등)로 가끔 해제
- **본 시스템 영향**: **없음** — 한도 내에서만 해제하므로 광고 노출 없음
- **만족도**: 유지 또는 향상 (일관된 경험)

### 3.2 Primary Persona B: "충동형 J"

- **프로필**: 20대 학생, un:short 1개월차, 일일 차단 해제 8~15회
- **현재 상태**: 30초 카운트다운을 견디며 반복 우회 시도
- **본 시스템 영향**: 한도 초과 후 광고 시청 → 자기 인식 trigger
- **기대 행동 변화**: (a) 광고 시청 후에도 풀어 광고 임프레션 발생, (b) 광고 부담으로 일부는 자제, (c) 일부는 프리미엄 전환

### 3.3 Secondary Persona: "프리미엄 G"

- **프로필**: 프리미엄 구독자, 광고 비노출 사용자
- **본 시스템 영향**: **없음** — 무제한 즉시 해제, 광고 없음
- **유의점**: 프리미엄 가치 제안 강화에 본 PRD가 기여

---

## 4. 범위 (Scope)

### 4.1 In Scope

| 대상 | 적용 시점 |
|---|---|
| `ShortsBlockOverlayActivity` (쇼츠 차단 오버레이) | v2.1 |
| `FeedBlockOverlayActivity` (피드 차단 오버레이) | v2.1 |

### 4.2 Out of Scope

- 일일 한도 사용자 커스터마이징
- 한도/광고 충전 횟수의 메인 화면 노출
- iOS 버전
- 다른 광고 위치(배너, 메인 인터스티셜) 정책 변경
- 광고 시청 횟수 제한 (광고 시청은 무제한 허용)

---

## 5. 정책 (Quota & Ad Charge Policy)

### 5.1 핵심 메커니즘 (v1.2)

**핵심 패러다임**: 별도 광고 게이트 화면이 아닌, **기존 차단 화면 그대로 + 하단에 옵션 영역 통합**. 한도 = "마찰 우회권"이며, 마찰 견디기는 한도 무관.

```
[차단 발생 (오버레이 진입)]
  ↓
[Intent에 EXTRA_ENTRY_FROM_SCROLL 전달]
  ↓
[ShortsBlockOverlayActivity가 직접 분기 + 한도 조회]
  │
  ├─ 스크롤 진입 → 하단 영역 비노출, 기존 차단 화면 그대로
  │   └ "계속 볼래요"(30초+폰뒤집기) 또는 "그만 볼래요" — 한도 차감 없음
  │
  └─ 클릭 진입 → 하단 영역 분기 노출:
        ├─ 프리미엄: [⚡ 바로 보기]                         (잔여 텍스트 X)
        ├─ 한도 ≥ 1: [⚡ 바로 보기] + "오늘 N회 남음"         (즉시 해제 + 한도 1 차감)
        └─ 한도 = 0: "오늘 한도 다 썼어요" + [📺 광고 보고 +3회]
              └ 광고 시청 완료 → 한도 +3 → UI를 "바로 보기 (오늘 3회 남음)"로 즉시 재구성

  메인 액션 (모든 진입 방식 공통, 항상 노출):
    "계속 볼래요" → 30초+폰뒤집기 → 해제 (한도 차감 X)
    "그만 볼래요" → 차단 유지, 홈으로
    ↓
[자정 00:00]
  └ 한도 10회로 리셋 (DailyUnblockQuotaResetReceiver, Lazy 보정 보강)
```

**한도 차감 트리거**: **"바로 보기" 클릭 1회 = -1**. "계속 볼래요"(30초+폰뒤집기)는 차감 없음.

**광고 보상 충전량**: 광고 1편 = **+3회** (Remote Config `unblock_quota_ad_recharge_amount`로 동적 조절).

**프리미엄 사용자**: 한도 무제한이므로 "바로 보기"가 항상 노출되며 차감 없음. 잔여 텍스트도 노출 X. 결과적으로 클릭 진입은 사실상 무제한 즉시 해제 = 기존 "스크롤한 쇼츠만 차단" 토글 ON과 효과적 등가.

**책임 분담**:
- `ShortsBlockService`: 진입 방식 캡처 → Intent로 전달, ACTION_INSTANT_UNBLOCK 수신 시 차단 해제 처리
- `ShortsBlockOverlayActivity`: 한도 조회/차감/충전 모두 직접 호출, UI 분기, 액션 브로드캐스트
- `DailyUnblockQuotaManager`: 한도 영속/조회/차감/충전, 자정 리셋
- `PremiumManager`: 프리미엄 상태 (기존)

### 5.2 한도 정책 (v1.2)

| 항목 | 값 | 비고 |
|---|---|---|
| 일일 기본 한도 | **10회** | 쇼츠 클릭 진입에 한정. Remote Config `unblock_quota_daily_limit` |
| 리셋 시점 | 매일 00:00 (디바이스 로컬 시간) | `DailyUnblockQuotaResetReceiver` + Lazy 보정 |
| 한도 차감 트리거 | **"바로 보기" 클릭만** | "계속 볼래요"(30초+폰뒤집기)는 차감 X |
| 광고 시청 시 충전량 | **+3회** (시청 1편당) | Remote Config `unblock_quota_ad_recharge_amount` |
| 광고 시청 횟수 제한 | 없음 | 사용자 의지에 따름 |
| 신규 사용자 그레이스 | 첫 7일 한도 = **15회** | Remote Config `unblock_quota_new_user_limit/grace_days` |
| 진입 방식 감지 | `prefsManager.isAllowedUntilScroll` (= `isCurrentSessionFromScroll`) | 기존 검증된 로직, Intent extra로 Activity에 전달 |
| 한도 차감 책임자 | **`ShortsBlockOverlayActivity`** (Activity 단일 소유) | 서비스는 한도 로직 보유 X |

### 5.3 광고 시청 정책 (v1.2)

| 항목 | 값 |
|---|---|
| 광고 형식 | Muuu Ad SDK 리워드 광고 (`MuuuRewardedAdLoader`, 스킵 불가) |
| 광고 길이 | SDK 기본값 (보통 15~30초) |
| 광고 로드 실패 시 | "광고를 불러올 수 없어요" 토스트 + 사용자가 다시 시도 가능 |
| 광고 시청 중 백그라운드 진입 | 광고 무효 처리 + 한도 충전 X |
| 광고 시청 보상 적용 시점 | `MuuuRewardedAdListener.onEarnedReward` 콜백 시점에 즉시 +3 |
| 광고 시청 후 UI 처리 | `setupBottomActionUi` 동기적 재호출로 즉시 "바로 보기 (오늘 3회 남음)" 상태로 전환 |

### 5.4 마찰 정책 (v1.2)

마찰은 **사용자가 어떤 액션을 선택하느냐**에 따라 적용 여부가 결정된다.

| 액션 | 30초 카운트다운 | 폰 뒤집기 | "정말 지금?" 메시지 | 한도 차감 |
|---|---|---|---|---|
| "계속 볼래요" | ✅ | ✅ | ✅ | ❌ |
| "그만 볼래요" | (즉시 차단 유지) | — | — | ❌ |
| "바로 보기" | ❌ (스킵) | ❌ (스킵) | ❌ (스킵) | ✅ (-1) |
| "광고 보고 +3회" → 광고 시청 → "바로 보기" | ❌ (스킵) | ❌ (스킵) | ❌ (스킵) | 충전(+3) 후 차감(-1) |

> **한도 = 마찰 우회권**. "바로 보기"는 한도를 소비해 마찰을 면제하는 액션. 한도가 없으면 광고 시청으로 +3회 충전.

> **마찰 견디기는 항상 무료**. "계속 볼래요"로 30초+폰뒤집기를 견디는 사용자는 의식적 자제 의지를 가진 사용자이므로 한도 차감 없이 자유롭다.

차단 앱 정체성 보존 원리:
- 빠르게 풀고 싶음 → 한도 소비 또는 광고 시청 (의도적 비용 지불)
- 천천히 풀고 싶음 → 마찰 견디기 (시간 비용 지불)
- 풀고 싶지 않음 → "그만 볼래요"

세 옵션 모두 사용자 자율적 선택.

### 5.5 프리미엄 차별화

| 사용자 유형 | 클릭 진입 한도 | 스크롤 진입 | 광고 노출 |
|---|---|---|---|
| 일반 (Free) | 10회 (신규 7일은 15회) | 한도 무관, 30초 마찰 | 한도 초과 시 |
| 프리미엄 | **무제한** | 한도 무관, 30초 마찰 | **없음** |

프리미엄 가치 제안: **"광고 없이 무제한 즉시 해제"**

#### 기존 "스크롤한 쇼츠만 차단" 토글과의 관계 (옵션 C: 유지 + 노출 결정 가능)

기능적 등가성으로 인해 별도 토글 없이도 본 시스템이 같은 효과를 자연 발생시키지만, **기존 코드와 정책은 유지**한다.

| 항목 | 처리 |
|---|---|
| `PREF_BLOCK_SCROLLED_ONLY` 코드 | **유지** (기능 그대로) |
| 토글 UI 노출 여부 | **Remote Config로 제어** (`feature_flag_show_block_scrolled_only_toggle`) |
| 출시 단계 기본값 | **숨김** (한도 시스템이 자동 등가 효과 제공) |
| 기존 토글 ON 사용자 마이그레이션 | 토글 상태 그대로 유지, 한도 시스템과 병렬 동작 |
| 토글 ON + 한도 시스템 동시 적용 시 | 토글 우선 (클릭 진입 시 차단 화면 자체 미노출 → 한도 차감 X) |

**노출 결정 가능**: 운영팀이 Remote Config 플래그로 토글의 사용자 노출을 ON/OFF 가능. A/B 테스트, 단계적 전환, 특정 사용자군 노출 등에 활용.

### 5.6 카운트 통합 정책

- 쇼츠 차단(`ShortsBlockOverlayActivity`)과 피드 차단(`FeedBlockOverlayActivity`)의 해제는 **동일 한도에서 차감**
- 사용자 인식: "쇼츠 7회 + 피드 3회 = 10회" 통합
- 별도 카운트로 분리하지 않음 (사용자 인지 부담 ↓)
- **단, 한도 차감은 클릭 진입 해제에만 적용**. 스크롤 진입 해제는 별도로 카운트하지 않음

#### 진입 방식 × 액션 차단 처리 매트릭스 (v1.2)

| 진입 방식 | 액션 | 하단 영역 노출 | 한도 차감 | 30초 마찰 | 폰 뒤집기 |
|---|---|---|---|---|---|
| 스크롤 | "계속 볼래요" | ❌ (영역 자체 비노출) | ❌ | ✅ | ✅ |
| 스크롤 | "그만 볼래요" | ❌ | ❌ | — | — |
| 클릭 | "계속 볼래요" | (노출되어 있음) | ❌ | ✅ | ✅ |
| 클릭 | "바로 보기" (한도 ≥ 1) | ✅ "바로 보기 (오늘 N회 남음)" | ✅ (-1) | ❌ | ❌ |
| 클릭 | "광고 보고 +3회" (한도 = 0) | ✅ "오늘 한도 다 썼어요" + 버튼 | 광고 후 +3 | ❌ | ❌ |
| 클릭 (프리미엄) | "바로 보기" | ✅ "바로 보기" (잔여 텍스트 X) | ❌ | ❌ | ❌ |
| 피드 차단 (스크롤 임계 도달) | "더 보기/그만 보기" | ❌ (영역 자체 비노출) | ❌ | ✅ (30초 타이머) | ❌ |

> **피드 차단은 본질적으로 "스크롤 누적 임계 초과"라는 스크롤성 트리거**이므로 한도/광고 적용 대상 외. 피드 차단 화면은 본 PRD에서 변경하지 않음.

#### 정책 변경 사항 (v1.2)

v1.1 대비 다음 정책이 변경되었다:
- 한도 차감 트리거: "더 볼래요" 시점 → **"바로 보기" 클릭 시점만**
- 광고 충전량: +1회 → **+3회**
- UX 모델: 별도 광고 게이트 화면 → **차단 화면 하단 영역 통합**
- 잔여 횟수 노출: 비노출 → **"오늘 N회 남음" 명시 표시**
- 마찰 정책: 광고 후에도 마찰 적용 → **"바로 보기"/광고 후 즉시 해제, "계속 볼래요"는 한도 무관 무료**
- 한도 책임자: 서비스의 OverlayActionReceiver → **`ShortsBlockOverlayActivity` 단일 소유**
- Intent 전달: OVERLAY_MODE 4가지 → **`EXTRA_ENTRY_FROM_SCROLL` 1개만, 동적 정보는 Activity가 직접 조회**
- 저장소: DataStore (가정) → **SharedPreferences** (프로젝트 일관성)

### 5.7 노출 원칙 (v1.2 정정)

| 위치 | 노출 |
|---|---|
| 메인 화면 | ❌ 표시 X (시스템 존재 비노출) |
| 차단 화면 (스크롤 진입) | ❌ 하단 영역 비노출 (기존 UI 그대로) |
| 차단 화면 (클릭 + 한도 ≥ 1) | ⭕ **"바로 보기 (오늘 N회 남음)" 명시** |
| 차단 화면 (클릭 + 한도 = 0) | ⭕ "오늘 한도를 다 썼어요" + "[광고 보고 3회 추가]" |
| 차단 화면 (클릭 + 프리미엄) | ⭕ "바로 보기" (잔여 텍스트 X — 무제한이므로) |
| 리포트 화면 | ⭕ "오늘 차단 해제: N회", "오늘 광고 시청: N편" (강조 X, 통계 일부) |
| 설정 화면 (한도) | ❌ 표시 X (초기 출시) |
| 기존 "스크롤한 쇼츠만 차단" 토글 | ❌ 숨김 (Remote Config 기본값), 노출 시점은 운영 결정 |

**v1.2 정책 변경**: v1.1의 "10회 숫자, 카운터 비노출" 정책을 정정. **차단 화면 하단의 "바로 보기" 버튼에서는 잔여 횟수를 명시 표시**한다. 사용자가 즉시 해제 권한이 얼마나 남았는지 의사결정 시점에 알 수 있어야 자기 인식 효과가 발생.

단, 게이미피케이션은 회피:
- ❌ 메인 화면 카운터/게이지/배지
- ❌ "오늘 N회 보너스 획득!" 같은 보상 알림
- ❌ "연속 N일 절제 챌린지" 같은 요소
- ✅ 차단 화면의 의사결정 정보로서의 잔여 횟수
- ✅ 리포트의 회고적 통계

---

## 6. 사용자 시나리오

### 6.1 시나리오 A: 클릭 진입 + 한도 ≥ 1

```
1. 사용자가 Instagram 홈에서 Reels 카드 탭 (클릭 진입)
2. ShortsBlockService.showBlockOverlay() → Intent에 EXTRA_ENTRY_FROM_SCROLL=false
3. ShortsBlockOverlayActivity 진입
   - intent에서 isFromScroll=false 읽음
   - DailyUnblockQuotaManager.getRemainingQuota()=7 직접 조회
   - 하단 영역 노출: "[⚡ 바로 보기] 오늘 7회 남음"
4. 사용자 옵션:
   (a) "바로 보기" 클릭 → 한도 -1, ACTION_INSTANT_UNBLOCK 브로드캐스트, 즉시 해제
   (b) "계속 볼래요" → 30초+폰뒤집기, 한도 차감 X, 해제
   (c) "그만 볼래요" → 차단 유지, 홈으로
```

### 6.2 시나리오 B: 클릭 진입 + 한도 = 0 + 광고 시청

```
1. 클릭 진입, 한도 0 상태
2. 차단 화면 진입 → 하단 영역에 "오늘 한도를 다 썼어요" + [📺 광고 보고 3회 추가]
3. 사용자가 [광고 보고 3회 추가] 클릭
4. AdManager.setupRewardedAd 호출 → MuuuRewardedAdLoader 광고 재생 (15~30초)
5. onEarnedReward 콜백
   - DailyUnblockQuotaManager.rechargeFromAd() 호출 → 한도 +3
   - setupBottomActionUi(isFromScroll=false) 동기 재호출
   - UI가 즉시 "[⚡ 바로 보기] 오늘 3회 남음"으로 전환
6. 사용자가 "바로 보기" 클릭 → 한도 -1 (남은 2회), 즉시 해제
```

### 6.3 시나리오 C: 스크롤 진입 (한도 무관)

```
1. 사용자가 Reels 시청 중 다음 Reels로 스와이프 (스크롤 진입)
2. ShortsBlockService.showBlockOverlay() → Intent에 EXTRA_ENTRY_FROM_SCROLL=true
3. ShortsBlockOverlayActivity 진입
   - isFromScroll=true → 하단 영역 GONE
   - 기존 차단 화면 그대로 ("계속 볼래요" / "그만 볼래요"만)
4. 한도 0 상태에서도 동일 (광고 충전 옵션 미노출)
5. "계속 볼래요" → 30초+폰뒤집기 → 해제, 한도 차감 X
```

### 6.4 시나리오 D: 광고 로드 실패

```
1. 한도 0 + 클릭 진입 + 비행기 모드
2. 사용자가 [광고 보고 3회 추가] 클릭
3. MuuuRewardedAdLoader.load → 실패
4. RewardResult.Failed 콜백 → 토스트 "광고를 불러올 수 없어요"
5. 사용자가 다시 시도 가능 (버튼 그대로 유지) 또는 "그만 볼래요"로 종료
6. 차단 본연 기능은 영향 X (Fail-Open)
```

### 6.5 시나리오 E: 자정 리셋

```
1. 23:55 - 한도 0 상태
2. 00:00 - DailyUnblockQuotaResetReceiver 트리거 → 한도 10 회복
   - 또는 알람 누락 시 다음 호출 시점 Lazy 보정
3. 00:01 - 차단 해제 시 정상 동작 (한도 10 → 9 또는 견디기)
```

### 6.6 시나리오 F: 프리미엄 사용자

```
1. 프리미엄 사용자, 클릭 진입 100회째
2. 차단 화면 → isFromScroll=false, PremiumManager.isPremium()=true
3. 하단 영역: "[⚡ 바로 보기]" (잔여 텍스트 없음)
4. "바로 보기" 클릭 → 한도 차감 X, 즉시 해제
5. 결과: 클릭 진입은 항상 자유롭게 해제 = 기존 "스크롤한 쇼츠만 차단" 토글 ON과 효과적 등가
   (단, 차단 화면 자체는 표시됨 — 토글 ON 시는 차단 화면이 아예 안 뜸)
```

### 6.7 시나리오 G: 프리미엄 사용자 + 기존 토글 ON 유지

```
1. 기존 "스크롤한 쇼츠만 차단" 토글 ON 상태인 프리미엄 사용자
2. 클릭 진입 시 → 토글 ON 정책 우선 적용 → 차단 화면 자체가 노출되지 않음
3. 한도 시스템에 도달하지 않음 (한도 차감 X, 광고 게이트 X)
4. 스크롤 진입 시 → 차단 화면 표시 → 30초 마찰 → 해제

→ Remote Config로 토글 노출이 ON되어 있고 사용자가 직접 활성화한 경우만 해당
→ 토글 OFF 시 기본 동작(시나리오 F)과 동일
```

---

## 7. 기능 요구사항 (Functional Requirements)

### 7.1 핵심 기능

| ID | 요구사항 | 우선순위 |
|---|---|---|
| FR-1 | 일일 즉시 해제 한도(기본 10회)를 DataStore에 영속 저장한다 | P0 |
| FR-2 | **클릭 진입** 차단 해제 시도 시 한도를 1 차감한다 (쇼츠 차단에 한정) | P0 |
| FR-2a | **스크롤 진입** 차단 해제는 한도를 차감하지 않는다 | P0 |
| FR-2b | `SessionStateManager.isCurrentSessionFromScroll` 값을 기반으로 진입 방식을 분기한다 | P0 |
| FR-2c | 피드 차단(`FeedBlockOverlayActivity`) 해제는 한도/광고 시스템을 적용하지 않는다 | P0 |
| FR-3 | 클릭 진입 + 한도 0 상태에서 차단 발생 시 광고 게이트 화면을 표시한다 | P0 |
| FR-4 | Muuu Ad SDK 리워드 광고를 로드/표시한다 | P0 |
| FR-5 | 광고 시청 완료(`onUserEarnedReward`) 시 한도를 +1 충전한다 | P0 |
| FR-6 | 광고 충전 후 자동으로 기존 차단 해제 플로우를 진행한다 | P0 |
| FR-7 | 광고 로드 실패 시 사용자에게 알리고 재시도 옵션을 제공한다 | P0 |
| FR-8 | 매일 자정(00:00)에 한도를 10으로 리셋한다 | P0 |
| FR-9 | 신규 설치 후 7일간 한도를 15로 부여한다 | P1 |
| FR-10 | 프리미엄 사용자는 한도 체크를 스킵한다 (무제한) | P0 |
| FR-11 | 스크롤 진입 시 차단 화면 하단 영역(`bottomActionContainer`)을 GONE 처리한다 (기존 UI 그대로) | P0 |
| FR-12 | 클릭 진입 + 한도 ≥ 1 시 하단에 "[⚡ 바로 보기] 오늘 N회 남음" 카드를 노출한다 | P0 |
| FR-12a | 클릭 진입 + 한도 = 0 시 하단에 "오늘 한도를 다 썼어요" + "[광고 보고 +3회]" 영역을 노출한다 | P0 |
| FR-12b | 클릭 진입 + 프리미엄 시 하단에 "[⚡ 바로 보기]"만 노출한다 (잔여 텍스트 X) | P0 |
| FR-13 | 메인 화면에 한도 정보를 노출하지 않는다 | P0 |
| FR-14 | 리포트 화면에 "오늘 차단 해제 N회"와 "오늘 광고 시청 N편" 통계를 표시한다 (강조 X) | P1 |
| FR-15 | 광고 시청 중 백그라운드 진입 시 보상 무효 처리한다 | P0 |
| FR-16 | "바로 보기" 클릭 시 30초+폰뒤집기 마찰을 모두 스킵하고 즉시 해제한다 | P0 |
| FR-16a | "계속 볼래요" 클릭 시는 기존대로 30초+폰뒤집기 후 해제한다 (한도 차감 X) | P0 |
| FR-17 | 한도 차감은 **"바로 보기" 클릭 시점**에만 수행한다 (Activity가 직접 호출) | P0 |
| FR-17a | 광고 시청 완료 시점에 한도 +N (Remote Config `unblock_quota_ad_recharge_amount`, 기본 3) 충전한다 | P0 |
| FR-17b | 광고 시청 후 `setupBottomActionUi`를 동기 재호출하여 UI를 즉시 재구성한다 | P0 |
| FR-18 | 한도 차감 실패(쓰기 오류 등) 시에도 차단 해제 플로우는 정상 진행한다 (Fail-Open) | P1 |
| FR-19 | 기존 `PREF_BLOCK_SCROLLED_ONLY` 토글 코드/정책을 유지한다 (이중 시스템) | P0 |
| FR-20 | 기존 토글의 UI 노출 여부를 Remote Config 플래그(`show_block_scrolled_only_toggle`)로 제어한다 | P0 |
| FR-21 | 기존 토글 ON 상태인 프리미엄 사용자는 클릭 진입 시 차단 화면을 노출하지 않는다 (한도 시스템 우회) | P0 |
| FR-22 | Remote Config 플래그 기본값은 OFF (토글 숨김)로 출시한다 | P0 |
| FR-23 | `ShortsBlockService.showBlockOverlay()`는 Intent에 `EXTRA_ENTRY_FROM_SCROLL`만 추가한다 (서비스는 한도 로직 보유 X) | P0 |
| FR-24 | `OverlayActionReceiver`에 `ACTION_INSTANT_UNBLOCK` 핸들러를 추가하여 `WatchConfirmed`와 동일한 차단 해제 처리를 수행한다 | P0 |
| FR-25 | 한도 데이터는 `PreferencesManager`(SharedPreferences)에 저장한다 (DataStore 미사용) | P0 |

### 7.2 비기능 요구사항

| ID | 요구사항 |
|---|---|
| NFR-1 | 한도 체크 로직이 차단 화면 진입 지연을 100ms 이상 유발하지 않는다 |
| NFR-2 | 광고 로드 실패가 차단 앱 본연 기능을 저해하지 않는다 (Fail-Open) |
| NFR-3 | 자정 리셋 누락(앱 비활성/디바이스 OFF) 시 다음 앱 진입 시 자동 보정한다 |
| NFR-4 | 한도 데이터는 디바이스 로컬에만 저장한다 (서버 미연동) |
| NFR-5 | 시간대 변경/일광절약시간 적용 시 자연스럽게 처리한다 (디바이스 로컬 자정 기준) |

---

## 8. 기술 아키텍처

### 8.1 모듈 구조 (v1.2)

```
app/src/main/java/com/muuu/unshort/
├── ad/
│   ├── AdManager.kt                              # 수정 - setupRewardedAd + RewardResult 추가
│   └── DailyUnblockQuotaManager.kt               # 신규 - 한도 관리 핵심 (SharedPreferences 기반)
├── receiver/
│   ├── DailyLimitResetReceiver.kt                # 기존 (재사용 패턴)
│   ├── DailyUnblockQuotaResetReceiver.kt         # 신규 - 자정 리셋 알람
│   └── AppRestartReceiver.kt                     # 수정 - 부팅/업데이트 후 재등록 추가
├── ui/activity/
│   ├── ShortsBlockOverlayActivity.kt             # 수정 - 하단 영역 분기, 한도 차감/충전 직접 처리
│   ├── ReportActivity.kt                         # 수정 - 통계 두 줄 추가
│   └── SettingsActivity.kt                       # 수정 - 토글 Remote Config 분기
├── feedblock/overlay/
│   └── FeedBlockOverlayActivity.kt               # 변경 없음 (한도/광고 시스템 적용 외)
├── prefs/
│   └── PreferencesManager.kt                     # 수정 - 한도 프로퍼티 추가
├── config/
│   └── AppConstants.kt                           # 수정 - 키 상수 추가
├── analytics/
│   └── AnalyticsEvent.kt                         # 수정 - 이벤트 상수 추가
├── ShortsBlockService.kt                         # 수정 - Intent extra + ACTION_INSTANT_UNBLOCK 핸들러
└── UnshortApplication.kt                         # 수정 - 자정 리셋 등록, install date 초기화
```

**리소스/매니페스트**:
- `app/src/main/res/layout/activity_shorts_block_overlay.xml` 수정 — `bottomActionContainer` 추가
- `app/src/main/res/xml/remote_config_defaults.xml` 수정 — 5개 플래그 기본값
- `app/src/main/res/values*/strings.xml` 17개 언어 — 신규 i18n 7개
- `app/src/main/AndroidManifest.xml` — `DailyUnblockQuotaResetReceiver` 등록
- `app/build.gradle.kts` — `MUUU_REWARDED_AD_UNIT_ID` BuildConfig 필드

### 8.2 데이터 모델

```kotlin
// PreferencesManager.kt에 추가되는 프로퍼티 (SharedPreferences 기반)
var dailyUnblockQuotaRemaining: Int          // 잔여 한도
var dailyUnblockQuotaLastResetDate: String   // "yyyy-MM-dd" 마지막 리셋 일자
var dailyUnblockQuotaInstallDate: String     // "yyyy-MM-dd" 신규 그레이스 판단용
var dailyUnblockTotalToday: Int              // 오늘 누적 해제 (통계)
var dailyAdWatchedToday: Int                 // 오늘 누적 광고 시청 (통계)

// DailyUnblockQuotaManager 내부 헬퍼
private fun isNewUserGracePeriod(context: Context): Boolean =
    ChronoUnit.DAYS.between(
        LocalDate.parse(prefsManager.dailyUnblockQuotaInstallDate),
        LocalDate.now()
    ) < remoteConfig.getInt(RC_UNBLOCK_QUOTA_NEW_USER_GRACE_DAYS)

private fun currentLimit(context: Context): Int =
    if (isNewUserGracePeriod(context))
        remoteConfig.getInt(RC_UNBLOCK_QUOTA_NEW_USER_LIMIT)
    else
        remoteConfig.getInt(RC_UNBLOCK_QUOTA_DAILY_LIMIT)
```

### 8.3 핵심 인터페이스 (v1.2)

```kotlin
// ad/DailyUnblockQuotaManager.kt
object DailyUnblockQuotaManager {
    /** 자정 리셋 보정 후 잔여 한도 반환 */
    fun getRemainingQuota(context: Context): Int

    /** "바로 보기" 클릭 시. 한도 1 차감. 프리미엄은 차감 X (내부 분기) */
    fun consumeOnInstantUnblock(context: Context)

    /** 광고 시청 보상으로 한도 +N 충전 (Remote Config: ad_recharge_amount, 기본 3) */
    fun rechargeFromAd(context: Context): Int  // 충전된 양 반환

    /** 자정 리셋 — DailyUnblockQuotaResetReceiver에서 호출 */
    fun resetDaily(context: Context)

    /** 통계 (리포트 화면용) */
    fun getTotalUnblockToday(context: Context): Int
    fun getTotalAdsWatchedToday(context: Context): Int

    /** Lazy 보정: 호출 시점에 lastResetDate 비교하여 자동 리셋 */
    private fun ensureFreshState(context: Context)
}
```

### 8.4 차단 화면 통합 흐름 (v1.2)

```kotlin
// ShortsBlockService.showBlockOverlay() — line 515 부근
isCurrentSessionFromScroll = prefsManager.isAllowedUntilScroll
// (기존 로직 그대로) ...
intent.putExtra(EXTRA_OVERLAY_TYPE, overlayType)
intent.putExtra(EXTRA_ENTRY_FROM_SCROLL, isCurrentSessionFromScroll)  // 신규 한 줄

// ShortsBlockService.OverlayActionReceiver.onReceive() — line 1100 부근
AppConstants.ACTION_INSTANT_UNBLOCK -> {
    val sourcePackage = intent.getStringExtra("source_package") ?: currentPackage
    // ACTION_WATCH_CONFIRMED와 동일 처리. 한도 차감은 Activity가 이미 처리.
    sessionState.handleEvent(SessionEvent.WatchConfirmed, sourcePackage)
    prefsManager.isAllowedUntilScroll = true
    val appConfig = AppBlockingRegistry.getConfigByPackageName(sourcePackage)
    if (appConfig?.controlsMedia == true) {
        handler.postDelayed({ resumeMedia() }, 400)
    }
}

// ShortsBlockOverlayActivity (Activity 단일 책임)
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... 기존 로직
    val isFromScroll = intent.getBooleanExtra(EXTRA_ENTRY_FROM_SCROLL, false)
    setupBottomActionUi(isFromScroll)
}

private fun setupBottomActionUi(isFromScroll: Boolean) {
    if (isFromScroll) {
        bottomActionContainer.visibility = View.GONE
        return
    }
    bottomActionContainer.visibility = View.VISIBLE

    val isPremium = PremiumManager.isPremium()
    val remaining = DailyUnblockQuotaManager.getRemainingQuota(this)

    when {
        isPremium -> showInstantUnblockButton(unlimited = true)
        remaining >= 1 -> showInstantUnblockButton(remaining = remaining)
        else -> showAdRechargeUi()
    }
}

private fun onInstantUnblockClicked() {
    DailyUnblockQuotaManager.consumeOnInstantUnblock(this)  // Activity가 직접 차감
    sendBroadcast(Intent(AppConstants.ACTION_INSTANT_UNBLOCK).apply {
        setPackage(packageName)
        putExtra("session_id", currentSessionId)
        putExtra("source_package", sourcePackage)
    })
    finish()
}

private fun onAdRechargeClicked() {
    AdManager.setupRewardedAd(this, rewardedAdUnit) { result ->
        when (result) {
            is RewardResult.Earned -> {
                DailyUnblockQuotaManager.rechargeFromAd(this)
                setupBottomActionUi(isFromScroll = false)  // UI 즉시 재구성
            }
            is RewardResult.Failed -> showLoadFailedToast()
            else -> { /* no-op */ }
        }
    }
}
```

### 8.4.1 진입 방식 감지 (기존 로직 활용)

기존 `SessionStateManager`의 스크롤 감지 로직(`isCurrentSessionFromScroll`)을 그대로 사용한다. 별도 검증 불필요.

| 상태 | 의미 |
|---|---|
| `isCurrentSessionFromScroll = true` | 사용자가 쇼츠 시청 중 다음 쇼츠로 스와이프하여 신규 세션 진입 |
| `isCurrentSessionFromScroll = false` | 사용자가 외부(홈/검색)에서 쇼츠를 클릭하여 신규 진입 |

`ShortsBlockService`의 `BlockingStage.WatchConfirmed` 전이 시점에 이미 스크롤 여부가 확정되며, `ShortsBlockOverlayActivity` 진입 시점에는 단순 조회만 수행한다.

### 8.4.2 Remote Config 플래그

```kotlin
// remote/RemoteFeatureFlags.kt
object RemoteFeatureFlags {
    /** 기존 "스크롤한 쇼츠만 차단" 토글의 사용자 노출 여부 */
    val showBlockScrolledOnlyToggle: Boolean
        get() = firebaseRemoteConfig.getBoolean("show_block_scrolled_only_toggle")

    // 기본값: false (출시 시 토글 숨김)
}
```

플래그가 OFF여도 기존 `PREF_BLOCK_SCROLLED_ONLY` 값은 유지된다. 이미 토글을 ON으로 설정한 기존 프리미엄 사용자는 UI에서 토글을 못 보더라도 기능은 계속 동작한다 (FR-19).

### 8.5 자정 리셋 메커니즘 (v1.2)

`DailyLimitResetReceiver` 패턴 100% 모방. 두 가지 보강 수단으로 자정 리셋 누락 방지:

1. **AlarmManager 기반 알람** (`DailyUnblockQuotaResetReceiver`)
   - `setExactAndAllowWhileIdle`로 매일 00:00 트리거 (Android 12+ `canScheduleExactAlarms` 폴백 처리)
   - 부팅/업데이트 후 재등록 (`AppRestartReceiver`에서 `scheduleReset()` 호출)

2. **Lazy 보정** (`getRemainingQuota` 호출 시점)
   - SharedPreferences에 저장된 `dailyUnblockQuotaLastResetDate`와 `LocalDate.now()` 비교
   - 다른 날짜면 한도 회복 후 반환
   - 알람 누락 시에도 사용자 체감 영향 없음

### 8.6 Muuu Ad SDK 연동 (v1.2 — 실제 SDK 시그니처)

`AdManager.kt`에 기존 `setupBannerAd`/`setupNativeAd`와 일관된 패턴으로 추가:

```kotlin
fun setupRewardedAd(
    activity: Activity,
    adUnit: MuuuRewardedAdUnit,
    onResult: (RewardResult) -> Unit
) {
    if (PremiumManager.isPremium()) {
        onResult(RewardResult.NotApplicable)
        return
    }

    val loader = MuuuRewardedAdLoader(activity, adUnit)
    loader.setListener(object : MuuuRewardedAdListener {
        override fun onShown(adInfo: MuuuAdInfo) { /* track */ }
        override fun onFailedToShow(adInfo: MuuuAdInfo, error: MuuuAdDisplayFailError) {
            onResult(RewardResult.Failed(error))
        }
        override fun onDismiss(adInfo: MuuuAdInfo) {
            // 보상 수령 없이 dismiss — 보상 적용 X
        }
        override fun onEarnedReward(adInfo: MuuuAdInfo, reward: MuuuRewardedAd.MuuuRewardedItem) {
            onResult(RewardResult.Earned)
        }
    })
    loader.load()
    loader.show(activity)
}

sealed interface RewardResult {
    data object Earned : RewardResult
    data class Failed(val error: MuuuAdDisplayFailError? = null) : RewardResult
    data object Cancelled : RewardResult
    data object NotApplicable : RewardResult  // 프리미엄 등 광고 비대상
}
```

> **SDK 지원 확인 완료**: Muuu Ad SDK 0.2.0은 `MuuuRewardedAdLoader`/`MuuuRewardedAdListener`/`MuuuRewardedAd.MuuuRewardedItem`을 제공한다. 내부적으로 Google AdMob `RewardedAd`를 래핑.

### 8.7 통계 및 로깅 (Amplitude, v1.2)

`AnalyticsEvent.kt`에 추가되는 이벤트 상수와 발생 시점:

| 이벤트명 | 속성 | 발생 시점 |
|---|---|---|
| `instant_unblock_clicked` | `entry_method`, `is_premium`, `remaining_after` | "바로 보기" 클릭 시 |
| `unblock_quota_consumed` | `remaining_after`, `is_premium` | `consumeOnInstantUnblock` 호출 후 |
| `unblock_quota_exhausted` | `total_unblock_today`, `entry_method` | 한도 0 도달 시 (광고 게이트 노출 직전) |
| `unblock_via_friction_no_charge` | `entry_method` | "계속 볼래요" (30초+폰뒤집기) 견디기 완료 시 |
| `ad_recharge_clicked` | (없음) | "광고 보고 +N회" 클릭 시 |
| `ad_recharge_ad_loaded` | `load_time_ms` | 광고 로드 성공 |
| `ad_recharge_ad_failed` | `error_code` | 광고 로드 또는 노출 실패 |
| `ad_recharge_ad_completed` | `watch_duration_ms` | 광고 시청 완료 (`onShown` ~ `onDismiss`) |
| `ad_recharge_quota_added` | `amount` (기본 3), `remaining_after` | `onEarnedReward` 콜백 후 |
| `daily_quota_reset` | `previous_remaining`, `previous_total`, `previous_ads_watched` | 자정 리셋 트리거 시 (Receiver 또는 Lazy 보정) |
| `block_scrolled_only_toggle_visible` | `is_visible` | Remote Config 플래그 변동 시 |
| `block_scrolled_only_toggle_changed` | `enabled` | 사용자 토글 변경 시 |

공통 속성:
- `entry_method`: "click" | "scroll"
- `is_premium`: bool
- `surface`: "shorts" (피드 차단은 본 시스템 적용 외)

---

## 9. UX/UI 사양 (v1.2)

### 9.1 차단 화면 — 기본 구조

기존 `activity_shorts_block_overlay.xml` 디자인 시스템(`primary_dark`, `accent_purple`, 88sp 타이머, 흰색 카드, 16dp cornerRadius) **유지**. 하단에 신규 영역(`bottomActionContainer`) 추가.

```
┌─────────────────────────────────┐
│   un:short                      │
│   잠깐만요! 🤚                   │
│                                 │
│   [00:30 카운트다운]             │
│   정말 지금 쇼츠를 봐야 할까요?    │
│                                 │
│   [계속 볼래요]    ← 30초+폰뒤집기, 한도 차감 X
│   [그만 볼래요]                  │
│                                 │
│   ─────────────────────         │  ← 신규 (조건부)
│   [하단 영역 - 진입/한도/프리미엄별 분기]
└─────────────────────────────────┘
```

### 9.2 하단 영역 분기

#### (a) 클릭 진입 + 한도 ≥ 1

```
   ┌─────────────────────────┐
   │  ⚡ 바로 보기            │
   │     오늘 7회 남음        │
   └─────────────────────────┘
```

- 즉시 해제 (30초 + 폰 뒤집기 모두 스킵)
- 한도 1 차감

#### (b) 클릭 진입 + 한도 = 0

```
   오늘 한도를 다 썼어요

   ┌─────────────────────────┐
   │  📺 광고 보고 3회 추가    │
   └─────────────────────────┘
```

- 광고 시청 → 한도 +3 충전 → UI 즉시 (a) 상태로 재구성
- 광고 로드 실패 시 토스트 "광고를 불러올 수 없어요" + 사용자 다시 시도 가능

#### (c) 클릭 진입 + 프리미엄

```
   ┌─────────────────────────┐
   │  ⚡ 바로 보기            │
   └─────────────────────────┘
```

- 잔여 횟수 텍스트 미노출 (무제한)
- 한도 차감 없음

#### (d) 스크롤 진입 (모든 사용자)

하단 영역(`bottomActionContainer`) 자체 GONE. 기존 차단 화면 그대로.

### 9.3 디자인 토큰

기존 디자인 시스템 그대로 사용:
- 배경: `primary_dark`
- 카드: 흰색, 16dp cornerRadius, 1dp stroke
- 강조 텍스트: `accent_purple`
- "바로 보기" 카드: 메인 액션과 동급 시각 비중 (사용자가 한 번에 인지)
- "광고 보고 +3회" 카드: 동일 톤이되 광고 아이콘으로 명시적 차별화
- "오늘 한도를 다 썼어요" 메시지: gray_600, 작게

### 9.4 리포트 화면 통합 (FR-14)

기존 리포트 통계 영역에 두 줄 추가 (강조 X):

```
오늘 차단 횟수: 12회
오늘 차단 해제: 8회   ← 신규
오늘 광고 시청: 2편   ← 신규
이번 주 평균: 6회
```

- 강조 색상 X (gray_700 등 일반 통계 톤)
- "8회 해제 = 마찰 견디기 + 바로 보기 + 광고 후 해제 합산" (구체 분류는 노출 X — 자기 인식은 사용자가)

### 9.5 메인 화면

**변경 없음.** 한도/광고 정보 일체 비노출. 시스템 존재 비노출.

### 9.6 설정 화면

**v2.1 출시 시 변경 없음.** 추후 한도 커스터마이징 노출은 별도 PRD.

---

## 10. Phase별 로드맵

### Phase 1: 핵심 시스템 구현 (v2.1)

**기간**: 1.5주

| 작업 | 산출물 |
|---|---|
| 1. Muuu Ad SDK 리워드 광고 지원 확인 / 업데이트 | SDK 버전 확정 |
| 2. `DailyQuotaDataStore` 구현 (Proto DataStore) | 한도 영속 저장 |
| 3. `DailyUnblockQuotaManager` 구현 | 한도 관리 API |
| 4. `RewardedAdLoader` 구현 | Muuu SDK 어댑터 |
| 5. `DailyQuotaResetReceiver` 구현 (AlarmManager) | 자정 리셋 |
| 6. `ShortsBlockOverlayActivity` 통합 (진입 방식 분기 + 한도 체크 + 광고 게이트) | `SessionStateManager.isCurrentSessionFromScroll` 활용 |
| 7. `FeedBlockOverlayActivity` 정책 분리 (한도/광고 미적용 명시) | 회귀 방지 가드 |
| 8. 광고 게이트 UI Layout (`activity_ad_gate.xml`) | un:short 디자인 시스템 적용 |
| 9. Amplitude 이벤트 로깅 (진입 방식 속성 포함) | 측정 인프라 |
| 10. Remote Config 플래그 추가 (`show_block_scrolled_only_toggle`, 기본 OFF) | 기존 토글 노출 제어 |
| 11. 설정 화면 토글 노출 분기 (`SettingsActivity` 수정) | Remote Config 기반 가시성 |
| 12. 다국어 처리 (17개 언어 strings.xml) | i18n |
| 13. 회귀 테스트 (기존 차단 플로우 + 스크롤한 쇼츠만 차단 토글 무결성) | QA |
| 14. Internal Beta 배포 | 출시 준비 |

### Phase 2: 측정 및 튜닝 (v2.1 출시 후 4주)

| 작업 | 결정 사항 |
|---|---|
| 1. 한도 도달률 측정 | 한도 값 조정 (5/10/15) |
| 2. 광고 시청 전환율 측정 | 광고 게이트 메시지 A/B 테스트 |
| 3. 삭제율 변화 측정 | 정책 유지/롤백 결정 |
| 4. 프리미엄 전환율 측정 | 프리미엄 안내 강화 여부 |

### Phase 3: 후속 검토 (Out of Scope)

- 한도 사용자 커스터마이징 노출
- 차단 성공 연속 N일 시 한도 +α 보너스 (게이미피케이션 위험 평가 필요)
- 광고 시청 횟수 일일 상한 (남용 방지 검토)
- 광고 콘텐츠 큐레이션 (제휴 디지털 디톡스 콘텐츠 우선 노출)

---

## 11. 리스크 및 대응

| 리스크 | 영향 | 발생 가능성 | 대응 방안 |
|---|---|---|---|
| Muuu Ad SDK 리워드 광고 미지원 | 구현 불가 | 중 | SDK 업데이트 또는 AdMob 별도 연동 검토 |
| 광고 인벤토리 부족으로 로드 실패 빈발 | 사용자 짜증, 차단 우회 불가 | 중 | Fail-Open: 로드 실패 시 일시적으로 한도 +1 부여 검토 (Phase 2) |
| 한도 10회가 너무 빡빡 | 일반 사용자 부정 경험 | 중 | 신규 7일 그레이스(15회) + 출시 후 데이터 기반 조정 |
| 한도 10회가 너무 느슨 | 광고 임프레션 미발생 | 중 | 출시 후 도달률 측정 후 8회로 하향 검토 |
| 자정 리셋 알람 누락 | 다음날 한도 0 잔존 | 저 | Lazy 보정으로 사용자 체감 영향 0 |
| 시간대 변경/해외 여행 | 자정 시점 혼란 | 저 | 디바이스 로컬 시간 기준 일관 처리 |
| 사용자가 "10회"라는 숫자를 원함 | 노출 압박 | 저 | 미니멀 원칙 유지, 리포트의 "차단 해제 N회"로 간접 인지 가능 |
| 광고 시청 후 마찰까지 적용에 사용자 반발 | "광고도 봤는데 또 30초?" | 중 | 광고 게이트 메시지에 "기다림 시간이 그대로 적용돼요" 명시 |
| 충동 사용자가 한도 시스템 인지 후 더 일찍 우회 시도 | 광고 임프레션 변동 | 저 | 4주 측정 후 행동 패턴 분석 |
| 프리미엄 사용자 대비 일반 사용자 차별 인식 강화로 부정 리뷰 | 평점 하락 | 저 | 광고 게이트 메시지 톤 부드럽게 + 프리미엄 가치 설명 명확화 |
| 데이터 손실(앱 강제 종료 등)로 한도 누적 오류 | 사용자 혼란 | 저 | DataStore 트랜잭션 안전성 + Fail-Open 정책 |
| `SessionStateManager.isCurrentSessionFromScroll` 오판정 (스크롤을 클릭으로 또는 그 반대) | 한도 차감 부정확 | 저 | 기존 검증된 로직 재사용. Amplitude 이벤트로 진입 방식 분포 모니터링, 이상 시점 발견 시 hotfix |
| 기존 "스크롤한 쇼츠만 차단" 토글 ON 사용자 동작 변경 인식 | 부정 리뷰 | 저 | 토글 ON 사용자에게는 기존 동작 그대로 유지 (FR-21). Remote Config 플래그 OFF 기본값으로 신규 노출 차단 |
| Remote Config 플래그 변경 시 사용자 혼란 (토글 갑자기 보임/사라짐) | UX 일관성 저하 | 중 | 플래그는 운영 결정 단위로만 변경. 변경 시 변경 이유와 대상 사용자군을 미리 정의 |
| 토글 ON + 한도 시스템 우선순위 충돌 | 정책 불일치 | 저 | FR-21에 따라 토글 ON 우선. ShortsBlockService 단계 분기로 구현 일관성 확보 |
| 진입 방식 분기로 인해 충동 사용자가 "스크롤로 들어가면 한도 안 깎인다"는 우회 학습 | 한도 시스템 효과 약화 | 중 | 스크롤 진입은 본질적으로 "이미 쇼츠 시청 중인 상태에서의 연속"이므로 우회 효과 제한적. 4주 측정 후 정책 재검토 |

---

## 12. Open Questions

1. **일일 한도 기본값 (10회 vs 다른 값)**:
   - 현재 사용자 일일 차단 해제 횟수 분포 분석 필요 (Amplitude)
   - 추천: 출시 전 데이터 기반으로 5/10/15 중 결정

2. **광고 시청 시 한도 충전량**:
   - 광고 1편 = +1회가 적정한지, 광고 1편 = +3회로 늘리면 임프레션 ↓ 만족도 ↑
   - 추천: +1회로 시작 후 측정

3. **광고 시청 횟수 일일 상한**:
   - 이론적으로 사용자가 광고를 무한 시청해 무제한 우회 가능
   - 추천: 초기엔 무제한, Phase 3에서 일일 광고 시청 20회 제한 검토

4. **신규 사용자 그레이스 기간 (7일 vs 14일)**:
   - 추천: 7일로 시작, 신규 사용자 만족도 데이터 보고 조정

5. **한도 0 상태에서 광고 거부 후 재시도 쿨다운**:
   - 동일 차단 화면에서 [내일 다시 시도] → HOME → 즉시 재진입 시 같은 광고 게이트 반복?
   - 추천: 제한 없음 (사용자 의지에 맡김)

6. **프리미엄 사용자에게 한도 통계 노출**:
   - "오늘 N회 해제" 통계는 프리미엄도 동일 노출?
   - 추천: 동일 노출 (자기 인식 가치는 모두에게 동일)

7. **Muuu Ad SDK eCPM 협상**:
   - 본 시스템은 광고주 입장에서 매력적 (스킵 불가 + 보상 명확)
   - eCPM 우선 인벤토리 협상 가능 여부

8. **A/B 테스트 인프라**:
   - 한도 5/10/15 동시 운영하여 정량 비교?
   - 추천: Phase 2에서 Remote Config 기반 분기

9. **기존 "스크롤한 쇼츠만 차단" 토글 노출 시점**:
   - Phase 1 출시 시 숨김(기본값 OFF), 어느 시점에 노출 결정할지?
   - 추천: 데이터 안정화 4주 후 (a) 영구 숨김(deprecate) (b) 프리미엄 사용자에게만 노출 (c) 전체 노출 중 선택

10. **피드 차단 한도 적용 여부 재검토**:
    - 본 PRD에서는 피드 차단을 한도 시스템에서 제외했음
    - 그러나 피드 차단도 "더 볼래요" 빈도가 충동성 지표일 수 있음
    - 추천: Phase 1 출시 후 피드 차단 "더 볼래요" 분포 측정, 임계점 식별 시 별도 PRD로 추가

11. **스크롤 진입 우회 학습 방지**:
    - 사용자가 "스크롤로 들어가면 무료다"를 학습하면 한도 시스템이 무력화될 수 있음
    - 추천: Amplitude로 사용자별 진입 방식 분포 추적, 비정상 패턴(스크롤 진입 비율 급증) 발견 시 정책 보강

---

## 13. 참고 자료

- 기존 광고 시스템: `app/src/main/java/com/muuu/unshort/ad/AdManager.kt`
- 차단 오버레이: `app/src/main/java/com/muuu/unshort/ui/activity/ShortsBlockOverlayActivity.kt`
- 피드 차단 오버레이: `app/src/main/java/com/muuu/unshort/feedblock/overlay/FeedBlockOverlayActivity.kt`
- 프리미엄 매니저: `app/src/main/java/com/muuu/unshort/premium/PremiumManager.kt`
- 피드 차단 PRD (자정 리셋 패턴 참조): `documents/prd/feed-block.md`
- Muuu Ad SDK: https://github.com/hsikkk/ad
- un:short 디자인 시스템: `CLAUDE.md`, `DESIGN_SYSTEM.md`

---

## 14. 변경 이력

| 일자 | 버전 | 변경 사항 | 작성자 |
|---|---|---|---|
| 2026-05-06 | v1.0 | 초안 작성 (광고 수익화 협의 기반) | hsikkk |
| 2026-05-06 | v1.1 | 진입 방식 분기 정책 도입 (스크롤 진입은 한도 차감 X). 기존 "스크롤한 쇼츠만 차단" 프리미엄 토글과의 통합 정책 명시 (옵션 C: 코드 유지 + Remote Config로 노출 제어). 피드 차단을 한도/광고 시스템 적용 대상에서 제외. 등가성 증명 및 관련 시나리오/FR/리스크/Open Questions 추가 | hsikkk |
| 2026-05-07 | v1.2 | UX 모델 전환: 별도 광고 게이트 화면 → 차단 화면 하단 영역 통합. 한도 = "마찰 우회권" 재정의 ("바로 보기" 클릭 시만 차감, "계속 볼래요"는 한도 무관). 광고 충전량 +1 → +3. 잔여 횟수 명시 노출 ("오늘 N회 남음"). 책임 분리: 서비스는 진입 방식만 Intent 전달, Activity가 한도 조회/차감/충전 단일 소유. 저장소: DataStore → SharedPreferences. Muuu SDK 0.2.0 리워드 광고 지원 확인 완료 (`MuuuRewardedAdLoader`). 신규 ACTION_INSTANT_UNBLOCK + EXTRA_ENTRY_FROM_SCROLL. FR-12/16/17 정정 + FR-23/24/25 추가. 시나리오/Amplitude 이벤트/모듈 구조 v3 plan과 일치하도록 정정 | hsikkk |
