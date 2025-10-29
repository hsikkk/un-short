# un:short Design System
## 미니멀 블랙 & 화이트 디자인 가이드라인

### 핵심 디자인 원칙
1. **극단적 미니멀리즘**: 필수 요소만 남기고 모든 장식적 요소 제거
2. **흑백 대비**: 검은색과 흰색만을 사용한 강렬한 대비
3. **공간의 활용**: 넉넉한 여백으로 숨 쉬는 디자인
4. **타이포그래피 중심**: 가늘고 섬세한 폰트로 우아함 표현

---

## 컬러 팔레트

### Primary Colors
- **Pure Black**: `#000000` - 헤더, 활성화 버튼
- **Pure White**: `#FFFFFF` - 배경, 비활성화 버튼
- **Transparent Black 30%**: `#4D000000` - 비활성화 아이콘

### Secondary Colors
- **Active Green**: `#00C853` - 활성 상태 표시 (작은 dot만)
- **Inactive Gray**: `#BDBDBD` - 비활성 상태 표시

### Text Opacity
- **Primary Text**: `1.0` (활성화), `0.4` (비활성화)
- **Secondary Text**: `0.8` (활성화), `0.3-0.4` (비활성화)
- **Hint Text**: `0.5`

---

## 타이포그래피

### Font Family
- **Primary**: `sans-serif-thin` - 메인 타이틀, 브랜드
- **Secondary**: `sans-serif` - 일반 텍스트
- **Labels**: `sans-serif-medium` - 상태 라벨

### Text Sizes
- **Brand Logo**: `26sp`
- **Main Status**: `14sp`
- **Description**: `12sp`
- **Small Label**: `11sp`
- **Large Number** (타이머): `88sp`

### Letter Spacing
- **Brand**: `-0.02`
- **Labels**: `0.08`
- **Regular**: `0.01`

---

## 레이아웃 구조

### 기본 화면 구성
```
┌─────────────────────────┐
│                         │
│    Black Header Zone    │ (브랜드, 설명)
│                         │
├─────────────────────────┤
│                         │
│                         │
│    White Content Zone   │ (주요 인터랙션)
│         (Center)        │
│                         │
│                         │
└─────────────────────────┘
```

### 패딩 & 마진
- **Header Padding**: `32dp` (좌우상하)
- **Content Padding**: `28dp`
- **Element Spacing**: `8dp`, `12dp`, `28dp` (계층별)

---

## 컴포넌트 스타일

### 1. Power Button (메인 토글)
```xml
- Size: 120dp x 120dp
- Shape: Perfect circle
- Active: Black background, 24dp elevation, white icon
- Inactive: White background, 8dp elevation, 30% black icon
- Animation: Scale 0.95 on press
```

### 2. Status Indicator
```xml
- Dot: 6dp circle (green/gray)
- Label: 11sp, sans-serif-medium, letterSpacing 0.08
- Format: "• ACTIVE" / "• INACTIVE"
```

### 3. Cards (온보딩, 설정 등)
```xml
- Background: Pure white
- Corner Radius: 16dp
- Elevation: 0dp (flat design)
- Border: None or 1dp light gray
- Padding: 24dp
```

### 4. Overlay (차단 화면)
```xml
- Background: 95% black (#F0000000)
- Card: White, centered, 40dp padding
- Timer: 88sp, bold, centered
- Message: 16sp, centered
```

### 5. Buttons
```xml
- Primary: Black background, white text
- Secondary: White background, black border, black text
- Disabled: 30% opacity
- Ripple: Light gray
```

---

## 애니메이션 & 인터랙션

### Transitions
- **Duration**: 150-300ms
- **Easing**: Material standard curve
- **Scale Press**: 0.95 scale down

### State Changes
- **Toggle**: Instant color swap with elevation change
- **Fade**: 200ms for text opacity changes

---

## 아이콘 가이드라인

### Style
- **Line Weight**: 1.5-2dp
- **Style**: Outline only, no filled icons
- **Size**: 24dp (standard), 36dp (power button)

### Colors
- **Active**: Pure white on black, or pure black on white
- **Inactive**: 30% opacity

---

## 화면별 적용 가이드

### MainActivity (메인 화면)
- 검은 헤더 + 흰 콘텐츠 영역
- 중앙 정렬 파워 버튼
- 상태 텍스트 + 인디케이터

### OnboardingActivity (온보딩)
- 전체 흰 배경
- 검은 텍스트
- 단계별 프로그레스: 검은 점/회색 점
- 버튼: 검은 배경 또는 검은 테두리

### BlockOverlay (차단 화면)
- 95% 검은 배경
- 흰색 중앙 카드
- 거대한 타이머 숫자
- 미니멀한 메시지

### Settings/Permissions (설정)
- 흰 배경
- 검은 섹션 헤더
- 토글: iOS 스타일 미니멀 스위치
- 리스트: 구분선 없이 패딩으로만 구분

---

## 접근성 고려사항

### Contrast Ratios
- Black on White: 21:1 (AAA)
- 30% Black on White: 2.7:1 (최소 사용)

### Touch Targets
- Minimum: 48dp x 48dp
- Power Button: 120dp (충분한 크기)

### Visual Feedback
- 모든 인터랙티브 요소에 ripple 효과
- 상태 변화 시 명확한 시각적 피드백

---

## 구현 체크리스트

- [ ] 모든 화면 흑백 컬러 스킴 적용
- [ ] sans-serif-thin 폰트 일관성 있게 사용
- [ ] 불필요한 장식 요소 제거
- [ ] 여백과 정렬 통일
- [ ] 애니메이션 최소화
- [ ] 그림자 효과 절제 (CardView elevation만 사용)