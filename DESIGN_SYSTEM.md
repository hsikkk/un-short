# un:short 디자인 시스템

- 상태: Active
- 구현 기준: `app/src/main/res/values/colors.xml`, `themes.xml`, `styles.xml` 및 각 화면 layout
- 마지막 코드 대조: 2026-08-02

이 문서는 디자인 의도를 설명합니다. 색상·폰트·컴포넌트의 정확한 값은 Android resource가 단일 기준이며, 값이 충돌하면 resource를 따릅니다.

## 원칙

1. 흑백과 그레이스케일을 중심으로 콘텐츠와 행동을 명확히 구분한다.
2. 성공·경고·오류 색상은 상태 전달에만 제한적으로 쓴다.
3. 장식보다 타이포그래피, 여백, 명확한 행동 순서를 우선한다.
4. 차단 화면의 마찰은 제품 동작이므로 시각 개선 과정에서 임의로 제거하지 않는다.

## 토큰

| 용도 | Resource | 현재 값 |
|---|---|---|
| Primary | `primary_dark` | `#000000` |
| Secondary dark | `primary_medium` | `#2D2D2D` |
| Surface | `white` / `gray_50` | `#FFFFFF` / `#FAFAFA` |
| Main text | `gray_900` | `#171717` |
| Secondary text | `gray_600` | `#525252` |
| Success | `success` | `#10B981` |
| Warning | `warning` | `#F59E0B` |
| Error | `error` | `#EF4444` |
| Overlay | `overlay_background` | `#F0000000` |

기본 글꼴은 `@font/spoqa_sans_neo`입니다. 텍스트 크기와 굵기는 화면의 정보 계층에 맞추되, 공통 값이 반복되면 먼저 style/resource로 승격합니다.

pen.dev 기준 디자인은 로컬 Spoqa Sans Neo를 직접 렌더링하지 못하므로 `Inter`를 시각적 대체 폰트로 사용합니다. Android 구현에서는 항상 `@font/spoqa_sans_neo`를 유지하며, 폰트 차이로 생기는 줄바꿈과 높이는 실제 앱에서 최종 확인합니다.

## 컴포넌트 기준

- 버튼: `Widget.App.Button`을 기본으로 사용하며 최소 48dp 터치 영역을 확보한다.
- 스위치: `Widget.App.Switch` 또는 동일한 공통 tint를 사용한다.
- 카드: 흰색 surface, 충분한 내부 여백, 최소한의 elevation/stroke를 사용한다.
- 상태 표시: 색상만으로 의미를 전달하지 않고 텍스트 또는 아이콘을 병행한다.
- 오버레이: 어두운 전면 배경, 높은 텍스트 대비, 한 화면에 하나의 명확한 주 행동을 유지한다.

## 화면별 의도

- 메인: 차단 상태, 핵심 통계, 앱별 설정 진입을 우선한다.
- 권한 설정: 필요한 권한과 완료 상태, 다음 행동을 한 단계씩 보여준다.
- 차단 오버레이: 현재 선택지와 남은 시간을 가장 높은 우선순위로 보여준다.
- 설정/리포트: 긴 콘텐츠를 스캔하기 쉽도록 섹션과 간격을 일관되게 유지한다.
- 프리미엄: 무료 기능을 모호하게 만들지 않으며 추가 혜택과 결제 상태를 분명히 구분한다.

## 접근성 체크리스트

- 본문과 핵심 컨트롤은 WCAG AA 수준의 대비를 목표로 한다.
- 모든 터치 대상은 최소 48dp × 48dp로 만든다.
- 비활성 상태를 낮은 투명도만으로 표현하지 않는다.
- 동적 카운트다운과 상태 변경은 TalkBack에서도 이해 가능한 라벨을 제공한다.
- 문자열을 layout/code에 직접 넣지 않고 string resource를 사용한다.

## 변경 절차

토큰 또는 공통 컴포넌트를 바꿀 때 resource와 이 문서를 같은 PR에서 수정합니다. 화면별 일회성 수치는 이 문서에 복제하지 않고 해당 layout을 기준으로 삼습니다. 문서 검증 절차는 [문서 운영 가이드](documents/README.md)를 따릅니다.
