# 운영 스크립트

## 문서 검사

저장소 루트에서 다음 명령을 실행합니다.

```bash
python3 scripts/check_docs.py
```

README의 버전·SDK·모듈 정보, Markdown 로컬 링크, 문서 속 개인 절대 경로를 검사합니다. 같은 검사는 GitHub Actions의 `Documentation` workflow에서도 실행됩니다.

## Lifetime Premium 프로모션 코드

프로모션 코드는 `generate_promo_codes.kts`를 기준 도구로 생성합니다. JDK와 Kotlin compiler가 필요합니다.

```bash
cd scripts
kotlinc -script generate_promo_codes.kts 5 FRIEND
```

- 첫 번째 인자: 생성 개수(1~100)
- 두 번째 인자: 코드 ID 접두사(기본값 `GIFT`)
- 출력: 현재 디렉터리의 `promo_codes_<timestamp>.csv`와 콘솔

코드 형식은 `UNSHORT-{ID}-{SIGNATURE}`이며 앱의 `PromoCodeValidator`와 동일한 알고리즘으로 검증됩니다.

### 취급 주의

- 생성된 CSV와 실제 코드는 인증 정보처럼 취급하고 커밋하지 않습니다.
- 로컬 검증 방식이므로 코드가 유출되면 다른 기기에서도 사용될 수 있습니다.
- 생성기와 앱 검증기의 형식·서명 로직을 변경할 때는 둘을 같은 변경으로 갱신하고 샘플 ID로 상호 검증합니다.
- `PromoCodeGenerator.kt`, `generate_codes.kt`, `generate_codes.py`는 기존 보조 도구입니다. 새 운영 작업은 위 Kotlin script를 사용합니다.
