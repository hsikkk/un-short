# un:short homepage

Korean/English responsive product homepage. Source: `dist/` with an English translation catalog in `locales/en.json`. The dependency-free Node build generates the English HTML and Vercel output.

- Content follows the app's existing Play listing and source behavior.
- Interactive web demonstration: block → timer → choice. The demo uses a button instead of a device sensor and compresses 30 seconds into six; this is disclosed next to the demo.
- Real app captures illustrate task lock and settings. Task lock and home-feed blocking are labeled Beta.
- No invented testimonials, download counts or effectiveness claims. No new analytics, account forms or external font requests.
- Store and contact links use the app's existing package ID and developer email.
- Static hosting configured by `.openai/hosting.json`. The homepage is tracked under sites/unshort-homepage in the Android app repository; the existing Sites deployment remains available.

Validation: JavaScript syntax; desktop and 390px/320px layouts; timer start/pause/resume/completion/reset; FAQ expansion; image loading and browser runtime errors.

## Vercel 배포

Vercel용 정적 배포 설정이 포함되어 있다. 의존성 설치 없이 Node.js의 기본 모듈만 사용한다.
`dist/`는 홈페이지 소스이고, 빌드 시 `.vercel-static/`에 배포 파일을 생성한다.
기존 Sites용 `.openai/hosting.json`은 보존한다. 빌드는 `dist/index.html`과 번역 사전에서 `dist/en.html`을 생성한다.

### CLI로 배포

이 홈페이지 폴더에서 실행한다. Android 저장소 루트에서 실행하지 않는다.

```sh
cd sites/unshort-homepage
vercel login
vercel             # Preview 배포
vercel --prod      # Production 배포
```

CLI가 프로젝트 위치를 물으면 현재 폴더(`./`)를 선택한다. 별도 프레임워크는 없다.
`vercel.json`에 빌드·출력 설정이 들어 있으므로 직접 입력할 필요가 없다.
계정/팀/프로젝트 연결 정보는 로컬 `.vercel/`에 저장되며 Git에서 제외한다.

### Git 저장소를 연결하는 경우

홈페이지는 기존 `hsikkk/un-short` 저장소의 `sites/unshort-homepage` 폴더에 포함되어 있다.
Vercel에서 해당 GitHub 저장소를 연결하고 Root Directory를 `sites/unshort-homepage`로 지정한다.
별도의 홈페이지 저장소나 서브모듈 설정은 필요 없다. 기존 Sites 배포 설정은 보존했다.

| 설정 | 값 |
|---|---|
| Root Directory | 홈페이지 전용 저장소라면 `.`; 일반 폴더로 포함된 모노레포라면 `sites/unshort-homepage` |
| Framework Preset | Other |
| Build Command | `node scripts/build-vercel.mjs` |
| Output Directory | `.vercel-static` |
| Install Command | 비어 있음 (설치 생략) |

### 도메인 및 검색 메타데이터

빌드 시 다음 순서로 canonical / Open Graph URL을 정한다.

1. `SITE_URL` — 직접 지정한 대표 주소. 예: `https://your-domain.com`
2. `VERCEL_PROJECT_PRODUCTION_URL` — Vercel의 프로젝트 대표 도메인
3. `VERCEL_URL` — 현재 배포 주소

기본 Vercel 도메인은 환경변수 추가 없이 사용할 수 있다. 시스템 환경변수 자동 노출을 껐다면
Vercel 프로젝트의 Environment Variables에 `SITE_URL`을 설정한다.
커스텀 도메인은 Vercel Domains에서 연결한 뒤 `SITE_URL`에도 지정하고 재배포한다.
주소는 경로나 쿼리가 없는 HTTPS 도메인이어야 한다.

Production에는 sitemap.xml과 검색 허용 robots.txt를 생성한다.
Preview/로컬 결과물에는 noindex와 검색 차단 robots.txt를 넣는다. 이는 접근 권한 제한 기능은 아니다.
기존 ChatGPT Sites 주소가 Vercel 결과물의 검색·공유 메타데이터에 남지 않도록 빌드에서 교체한다.

### 로컬 확인

```sh
node scripts/build-vercel.mjs
python3 -m http.server 4173 --directory .vercel-static
```

환경변수 없이 로컬 빌드하면 canonical 없이 생성한다. 도메인 주입까지 확인하려면:

```sh
SITE_URL=https://your-domain.com VERCEL_ENV=production node scripts/build-vercel.mjs
```

Vercel 계정 연결이나 배포 자체는 아직 실행하지 않았다.

공식 문서: [vercel.json](https://vercel.com/docs/project-configuration/vercel-json),
[시스템 환경변수](https://vercel.com/docs/environment-variables/system-environment-variables).

## 홈페이지 언어

- 한국어: `/`, 영어: `/en`. 헤더의 언어 링크로 전환한다. JavaScript가 없어도 본문과 링크를 사용할 수 있다.
- 언어별 URL을 유지하며 브라우저 언어에 따른 강제 리다이렉트는 하지 않는다.
- 영어 본문·접근성 문구는 `locales/en.json`, 체험 동작 문구는 `dist/app.js`에서 관리한다.
- 한국어 원본은 `dist/index.html`이다. 번역 키는 원문과 정확히 일치해야 하며, 수정 후 `node scripts/check-seo.mjs`로 누락을 확인한다.
- `node scripts/build-locales.mjs`로 영어 HTML만 재생성할 수 있다. `dist/en.html`은 직접 편집하지 않는다.
- 앱 화면 이미지는 한국어 캡처를 유지하며 영어 페이지에 명시한다. 앱의 17개 언어는 홈페이지에 자동 적용되지 않는다.

## SEO

각 언어 페이지에 독립적인 title/description, canonical, 상호 hreflang(`ko`, `en`, `x-default`), Open Graph/Twitter 공유 정보와 SoftwareApplication JSON-LD를 생성한다. 구조화 데이터에는 확인되지 않은 평점이나 가격을 넣지 않는다.

Production 빌드의 sitemap.xml에는 `/`와 `/en`이 포함된다. Preview 및 로컬 빌드는 두 언어 모두 noindex이며 sitemap을 생성하지 않는다. `cleanUrls`와 `trailingSlash: false` 설정에 맞춰 영어 canonical은 `/en`이다.

```sh
node scripts/check-seo.mjs
```

이 검증은 production/preview/로컬 빌드, 언어별 canonical/hreflang, 영어 번역 누락, 구조화 데이터, sitemap 및 잘못된 도메인 입력을 확인한다. 마지막에는 로컬 noindex 결과물을 남긴다. 일반 Python 정적 서버에서는 영어를 `/en.html`로 열 수 있고, 실제 Vercel에서는 `/en`으로 제공된다.

커스텀 도메인 확정 후 `SITE_URL`을 해당 HTTPS 도메인으로 지정하고 production 배포한다. 검색 콘솔 소유권 확인과 sitemap 제출은 별도이며, 아직 수행하지 않았다. 검색 순위나 리치 결과 노출은 보장되지 않는다.

설정 기준: [Google 다국어 페이지 안내](https://developers.google.com/search/docs/specialty/international/localized-versions), [SoftwareApplication](https://schema.org/SoftwareApplication).
