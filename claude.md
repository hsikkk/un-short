# un:short 디자인 컨셉

## 브랜드 아이덴티티

### 앱 이름
**un:short** - 콜론(:)을 활용한 심플하고 임팩트 있는 네이밍
- 쇼츠(shorts)의 반대 개념을 표현
- "언쇼트(unshort)" - 짧은 콘텐츠 중독에서 벗어나는 의미

### 핵심 메시지
"쇼츠 중독에서 벗어나세요. 의도적인 소비를 위한 작은 습관."

## 디자인 컨셉

### 1. 미니멀리즘 + 프리미엄
- 깔끔하고 정돈된 인터페이스
- 불필요한 요소 배제
- 여백을 활용한 시각적 여유
- 고급스러운 타이포그래피

### 2. 컬러 시스템

#### Primary Colors (다크 베이스)
- `primary_dark` #1A1A1A - 헤더 배경, 브랜드 컬러
- `primary_medium` #2D2D2D
- `primary_light` #424242

#### Accent Colors (생동감)
- `accent_purple` #8B5CF6 - 메인 액센트, 타이머 강조
- `accent_purple_light` #A78BFA - 보조 강조
- `accent_blue` #3B82F6 - 오버레이 권한 섹션 강조

#### Status Colors (직관적 피드백)
- `success` #10B981 - 완료/활성화 상태
- `warning` #F59E0B - 경고
- `error` #EF4444 - 에러/필수 액션

#### Neutral Grayscale
- Gray 50~900 스케일로 섬세한 계층 표현
- `gray_50` #FAFAFA - 배경
- `white` #FFFFFF - 카드 배경

### 3. 타이포그래피

#### 브랜드 타이틀
- **un:short**: sans-serif-black, 40sp/24sp
- 볼드하고 강렬한 존재감
- letterSpacing 0.02로 약간의 여유

#### 계층 구조
- **타이틀**: 16-26sp, sans-serif-medium, bold
- **본문**: 13-16sp, regular
- **라벨/보조**: 13-14sp, medium

### 4. 레이아웃 구조

#### MainActivity (설정 화면)
1. **헤더 섹션** (`primary_dark` 배경)
   - 브랜드 로고 (un:short)
   - 설명 문구
   - 32dp padding으로 넉넉한 공간

2. **콘텐츠 영역** (ScrollView)
   - **준비 완료 카드** (조건부 표시)
     - 큰 ✓ 체크마크 (48sp)
     - Success 컬러 강조
     - 2dp stroke로 강조

   - **권한 설정 카드**
     - 16dp 라운드 코너
     - 섹션별 4dp 컬러 바 (purple/blue)
     - 1dp stroke로 경계 구분
     - 각 섹션 분리선 (1dp, gray_200)

   - **차단 대상 앱 안내 카드**
     - 심플한 불렛 리스트
     - YouTube Shorts, Instagram Reels, TikTok

#### 오버레이 차단 화면
1. **전면 반투명 배경** (`overlay_background` #F0000000)

2. **중앙 카드**
   - 흰색 배경, 8dp elevation
   - 40dp 내부 패딩
   - 모든 요소 중앙 정렬

3. **콘텐츠 구조**
   - 브랜드 로고 (24sp, `primary_dark`)
   - 메인 타이틀: "잠깐만요! 🤚" (26sp, bold)
   - 설명 메시지 (16sp, gray_600)
   - **Flip 상태 배지**: 빨간 점 + 텍스트
   - **타이머**: 88sp, 초대형, `accent_purple`, bold
   - "초 남음" 라벨 (18sp, -8dp margin으로 밀착)
   - 구분선 (3dp, 80dp width, purple light)
   - 반성 메시지: "정말 지금 쇼츠를 봐야 할까요?"

### 5. Material Design 컴포넌트

#### Cards
- `MaterialCardView` 사용
- 16dp cornerRadius
- 0dp elevation (플랫 디자인)
- 1-2dp stroke로 경계 표현

#### Buttons
- `Widget.App.Button` 스타일
- Material Design 가이드라인 준수

### 6. 인터랙션 디자인

#### 권한 설정 흐름
1. 두 권한 중 하나라도 미완료 시 → 설정 카드 표시
2. 권한 완료 시:
   - 체크마크(✓) 추가
   - Success 컬러로 변경
   - 버튼 숨김
3. 모든 권한 완료 → 준비 완료 카드로 전환

#### 차단 화면 경험
- 전체 화면 덮개 (clickable, focusable)
- 거대한 타이머 숫자로 시선 집중
- "정말 지금 쇼츠를 봐야 할까요?" 메시지로 반성 유도
- 폰 뒤집기 액션으로 의도적 마찰(friction) 생성

## 디자인 철학

### 마찰(Friction)을 통한 의도성
쇼츠 접근에 적절한 불편함을 추가하여:
- 충동적 소비 방지
- 의식적인 선택 유도
- 30초의 반성 시간 제공

### 긍정적 피드백
- 완료 상태를 명확히 표시
- Success 컬러로 성취감 강조
- "차단 준비 완료" 메시지로 안심감

### 미니멀하지만 친근하게
- 이모지 사용 (🤚, ✓) - 감정적 연결
- 부드러운 메시지 톤
- 명확하고 간결한 안내

### 심리적 개입
- "정말 지금 쇼츠를 봐야 할까요?" - 자기 성찰 유도
- 30초 타이머 - 습관 패턴 단절
- 폰 뒤집기 - 물리적 액션을 통한 의식화
