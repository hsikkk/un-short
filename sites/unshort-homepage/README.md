# un:short homepage

Korean, buildless responsive product homepage. Serve `dist/` with a static server.

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
기존 Sites용 `.openai/hosting.json`과 `dist/`는 변경하지 않는다.

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

현재 홈페이지는 한국어만 제공한다. 앱의 17개 언어 번역은 홈페이지에 자동 적용되지 않는다.
브라우저 언어 감지 및 언어 선택 메뉴는 아직 구현하지 않았다.
