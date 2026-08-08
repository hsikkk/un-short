# 기능 프로토타입

아직 구현되지 않은 화면과 흐름을 기능별 `.pen` 파일로 관리합니다.

규칙:

- `design/unshort.pen`을 입력으로 복제해 공통 토큰과 컴포넌트를 재사용합니다.
- 파일 이름은 기능 단위 kebab-case를 사용합니다. 예: `adaptive-wait-time.pen`
- 최상위 frame에 `Proposed`를 표시하고 관련 PRD 경로를 기록합니다.
- 프로토타입은 현재 제품 스펙으로 간주하지 않습니다.
- 구현이 완료되면 해당 화면을 `design/unshort.pen`에 반영하고 `SCREEN_INVENTORY.md`를 `Current`로 갱신합니다.

```bash
pen --in design/unshort.pen \
  --out design/prototypes/<feature>.pen \
  --repo . \
  --prompt "기준 컴포넌트를 재사용해 <feature> 프로토타입을 만들고 모든 신규 frame을 Proposed로 표시"
```
