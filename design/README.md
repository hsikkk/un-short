# un:short 디자인 워크플로

`unshort.pen`은 현재 구현된 주요 화면의 시각적 기준입니다. 제품 동작은 `documents/SPEC.md`, 정확한 Android 구현은 XML/resource가 최종 기준입니다.

- 상태: Main·Settings actual-render verified, 나머지 화면 검증 중
- pen.dev file format: 2.15
- 기준 CLI 확인 버전: 0.3.1
- 마지막 Android 대조: 2026-08-02

## 파일 역할

- `unshort.pen`: Android 실기 렌더와 대조 중인 기준 화면 및 공통 컴포넌트
- `prototypes/`: 아직 구현되지 않은 기능 탐색본
- `SCREEN_INVENTORY.md`: pen frame과 Android 화면 대응표

기준 모바일 viewport는 `412×915`입니다. 별도의 compact variant는 관리하지 않습니다. 한 화면 안에서 세로 스크롤되는 UI는 첫 viewport를 보여 주는 기준 frame과 전체 콘텐츠를 펼친 `Full Scroll` companion frame을 함께 둡니다.

새 화면은 XML 값만으로 `Current`로 등록하지 않습니다. `1080×2400 @ 420dpi` 에뮬레이터에서 실제 렌더링을 캡처하고, 412dp viewport로 축소 대조해 정렬·간격·색상 검수를 통과한 뒤 등록합니다.

현재 검증 reference는 `references/android-412dp/main.png`와 `references/android-412dp/settings-list-verified-20260802.png`입니다. `references/android-412dp/settings.png`는 권한 미설정 상태 캡처이며 Settings 리스트 기준으로 사용하지 않습니다.

## 기능 개발 흐름

1. `unshort.pen`의 컴포넌트와 토큰을 재사용해 `prototypes/<feature>.pen`을 만듭니다.
2. 프로토타입에 `Proposed` 상태와 관련 PRD를 표시합니다.
3. 디자인 승인 후 Android XML/resource와 `documents/SPEC.md`를 구현합니다.
4. 실제 화면과 디자인을 비교하고 차이가 의도된 것인지 PR에 기록합니다.
5. 출시 동작이 확정되면 기준 frame을 `unshort.pen`에 반영합니다.

## 변경 규칙

- 현재 화면을 바꾸면 `.pen`, XML/resource, SPEC을 같은 PR에서 검토합니다.
- 스크롤 콘텐츠가 바뀌면 해당 기준 frame과 `Full Scroll` companion frame을 함께 갱신합니다.
- 색상과 공통 스타일은 Android resource와 `DESIGN_SYSTEM.md`를 기준으로 합니다.
- 실험안은 기준 파일에 바로 덮어쓰지 않고 `prototypes/`에서 시작합니다.
- `.pen` 파일과 함께 리뷰 가능한 PNG export를 PR에 첨부할 수 있지만, 반복 생성물은 기본적으로 커밋하지 않습니다.
- 개인 데이터나 실제 결제·프로모션 정보를 디자인 fixture에 넣지 않습니다.

## CLI 사용

```bash
pen --in design/unshort.pen --out design/prototypes/feature.pen \
  --repo . \
  --prompt "기준 컴포넌트를 재사용해 기능 프로토타입을 추가하고 Proposed로 표시"

pen --in design/unshort.pen --export /tmp/unshort-design.png --export-scale 1
```

CLI 실행에는 활성화된 pen.dev 계정과 네트워크 연결이 필요합니다.

처음 사용하는 환경에서는 `pen --list-workspaces`로 slug를 확인하고 `pen --workspace <slug>`로 현재 workspace를 선택합니다. `--workspace`는 로컬 경로가 아니며 로컬 저장소는 `--repo .`로 전달합니다.
