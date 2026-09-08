# 마음숨길 (Maeum Sumgil) — 프론트엔드 프로젝트 개요

> 공황장애 환자를 위한 강원도 "터널 회피" 안심 관광 서비스의 **화면 초안(UI 프로토타입)**.
> 백엔드·인증·실제 지도 연동 없이 전부 프론트엔드 mock 데이터로 동작하는 상태입니다.
> 해당 문서 내의 모든 내용은 프론트엔드의 구조를 파악한 것이다.

---

## 1. 기술 스택

| 구분 | 내용 |
|---|---|
| 프레임워크 | React 18 (`react`, `react-dom`) |
| 라우팅 | `react-router-dom` v6 (`BrowserRouter`) |
| 빌드 도구 | Vite 6 + `@vitejs/plugin-react` |
| 스타일링 | CSS 프레임워크 없음. 모든 컴포넌트가 inline `style={{ ... }}` 사용, `src/index.css`에 정의된 CSS 커스텀 프로퍼티(디자인 토큰)를 `var(--...)`로 공유 |
| 상태 관리 | 없음. 페이지별 로컬 `useState`/`useRef`만 사용. 전역 스토어, context, 쿼리 라이브러리 전무 |
| 데이터 소스 | `src/data/mock.js`, `src/data/gangwonGeo.js` — 하드코딩된 정적 배열/객체. API 호출·fetch 레이어 없음 |
| 테스트/린트 | 미설정 (`package.json`에 `dev`, `build` 스크립트만 존재) |
| 폰트/외부 리소스 | Pretendard(CDN), IBM Plex Mono(Google Fonts) — `index.html`에서 로드 |

---

## 2. 폴더 구조

```
mungsmu/
├─ index.html                  # Vite 엔트리, 폰트 로드, #root
├─ vite.config.js
├─ package.json
└─ src/
   ├─ main.jsx                 # ReactDOM.createRoot → <App/>
   ├─ App.jsx                  # 라우팅 정의 (아래 3장 참고)
   ├─ Layout.jsx                # 상단 헤더 + 네비게이션 + <Outlet/>
   ├─ index.css                 # 디자인 토큰(색상/반경/그림자) + 전역 리셋
   ├─ components/
   │  └─ GangwonMap.jsx          # 강원 18개 시군 SVG choropleth 지도
   ├─ data/
   │  ├─ mock.js                 # COURSES, TUNNELS, REGIONS, GRADE, DIFF
   │  └─ gangwonGeo.js            # 18개 시군 SVG path 좌표 + viewBox
   └─ pages/
      ├─ HomePage.jsx
      ├─ CoursesPage.jsx
      ├─ CourseDetailPage.jsx
      ├─ TunnelsPage.jsx
      ├─ TunnelDetailPage.jsx
      ├─ RoutePage.jsx
      ├─ NavigatingPage.jsx
      ├─ CompanionPage.jsx
      └─ MyLoginPage.jsx          # MyPage + LoginPage 두 컴포넌트를 한 파일에서 export
```

---

## 3. 라우팅 구조 (`src/App.jsx`)

```
/login                 → LoginPage          (Layout 미적용, 전체화면)
/companion              → CompanionPage      (Layout 미적용, 전체화면)
/navigating              → NavigatingPage     (Layout 미적용, 전체화면)

<Layout> 하위 (상단 헤더+네비 포함)
  /            → /home 로 redirect
  /home                → HomePage
  /courses             → CoursesPage
  /courses/:id          → CourseDetailPage
  /tunnels             → TunnelsPage
  /tunnels/:id          → TunnelDetailPage
  /route               → RoutePage
  /my                  → MyPage

*                    → /home 로 redirect (404 없음)
```

**설계 원칙**: `/login`, `/companion`, `/navigating`은 몰입형 플로우(로그인, 호흡가이드 세션, 실주행 내비게이션)라서 상단 네비 없이 전체화면으로 렌더링됨. 새로운 몰입형 화면을 추가할 때도 이 패턴을 따르는 것이 일관적.

**페이지 간 데이터 전달**: 전역 상태 없이 `react-router-dom`의 `location.state`와 `navigate(path, { state })`로 다음 화면에 필요한 데이터를 넘김. 예:
- `CourseDetailPage` → `/route` 이동 시 `{ dest: course.region }` 전달
- `RoutePage` → `/navigating` 이동 시 `{ origin, dest, durationMin, distanceKm, tunnel? }` 전달
- `NavigatingPage` → `/companion` 이동 시 `{ tunnel }` 전달
- `HomePage`의 지도 클릭은 `state`가 아니라 쿼리스트링(`/courses?region=춘천`)으로 전달 (CoursesPage가 `useSearchParams`로 초기 필터 값을 읽음)

---

## 4. 공용 레이아웃 — `Layout.jsx`

- `position: sticky` 상단 헤더: 로고("숨" 아이콘 + "마음숨길 · MAEUM SUMGIL" 텍스트) 클릭 시 `/home` 이동
- 네비게이션 5개 항목: 홈 / 안심 코스 / 터널 백과 / 길찾기 / 마이페이지 (`NavLink`로 active 스타일 적용)
- 우측 "동반 모드" 버튼: `/companion`으로 즉시 이동 (터널 정보 없이 진입 시 `CompanionPage` 내부의 `DEFAULT_TUNNEL`(미시령터널)이 기본값으로 쓰임)
- 우측 상단 아바타 원형(이니셜 "서") — 클릭 동작 없음, 프로필 표시용 placeholder
- `<Outlet/>`으로 하위 라우트 렌더링

---

## 5. 디자인 시스템 — `src/index.css`

전역 리셋 + CSS 변수(`:root`)로 토큰화:

| 카테고리 | 변수 예시 |
|---|---|
| 브랜드 컬러 | `--primary`(#14807A), `--primary-deep`, `--primary-bg`, `--accent` |
| 배경/보더 | `--bg-page`, `--bg-surface`, `--bg-subtle`, `--border`, `--border-light` |
| 텍스트 | `--text-head`, `--text-body`, `--text-sub`, `--text-muted` |
| 등급 컬러(3단계) | `--color-grade-green/-bg/-border`, `--color-grade-amber/...`, `--color-grade-red/...` |
| 레이아웃 | `--max-w`(1180px) |
| 반경 | `--r-sm/md/lg/xl` (8~16px) |
| 그림자 | `--shadow-sm/md/lg` |
| 폰트 | `--font-mono` (IBM Plex Mono, 라벨/캡션용) |
| 애니메이션 | `@keyframes pulse`(보호자 공유 인디케이터), `@keyframes ripple`(호흡 가이드 파동) |

모든 페이지가 이 토큰을 `var(--...)`로 재사용하며, 새 색상/반경을 하드코딩하지 않는 것이 기존 컨벤션.

---

## 6. 데이터 모델 (`src/data/mock.js`)

실제 백엔드가 없는 상태로, 아래 구조가 **사실상의 스키마 초안** 역할을 함.

### `COURSES` (안심 코스, 4건)
```js
{
  id, title, region, grade,        // grade: 'green' | 'amber' | 'red'
  distance,                        // 문자열 ("약 22km")
  safetyScore,                     // 0~100 정수
  tags: [...],                     // 필터링에 사용 ('터널 0', '해안' 등)
  summary,
  spots: [{ name, type, desc }],   // 경유지 목록
}
```

### `TUNNELS` (터널, 6건)
```js
{
  id, name, road, lengthM, lanes,
  ventGrade,      // '우수' | '양호' | '보통' (환기 등급, 텍스트)
  congestion,     // '낮음' | '보통' | '높음'
  diff,           // 1~5, 공황 난이도
}
```

### `REGIONS` (강원 18개 시군)
```js
{ name, grade, col, row, tunnels }   // col/row는 구 버전 그리드 좌표(현재 지도는 SVG geo 데이터 사용, col/row는 미사용 추정)
```

### `GRADE` / `DIFF` (등급 메타데이터, 매핑 테이블)
```js
GRADE = { green: {label:'안심', color, bg, border}, amber: {...}, red: {...} }
DIFF  = { 1: {color}, 2: {color}, 3: {color}, 4: {color}, 5: {color} }
```

### `gangwonGeo.js`
- `GANGWON_VIEWBOX = { w: 640, h: 450.6 }`
- `GANGWON_GEO`: 18개 시군 이름 → `{ d(SVG path), labelX, labelY }` — KOSTAT 2018 행정구역 경계 단순화 데이터

---

## 7. 컴포넌트 상세

### `GangwonMap.jsx`
- props: `selected`(선택된 시군 이름), `onSelect(name)`
- 하나의 `<svg viewBox>` 안에서 **2-pass 렌더링**:
  1. 1st pass: 18개 시군 `<path>` (등급별 fill/border, 클릭 시 `onSelect` 호출, hover 시 `brightness` 필터)
  2. 2nd pass: 모든 라벨(`시군명` + `터널 N`)을 별도 `<g pointerEvents:none>`에 그려 다른 시군 도형 위로 항상 보이게 함
- `STRETCH = 1.15`: 지도를 세로로 15% 늘려 화면 비율을 보정(`scaleY`), `labelTransform()`으로 라벨 텍스트에는 역스케일(`scale(1, 1/STRETCH)`)을 걸어 글자만 원래 비율 유지
- `<title>` 태그로 네이티브 브라우저 툴팁(시군명 · 터널 수) 제공

---

## 8. 페이지별 상세 기능

### 8-1. `HomePage.jsx`
- 히어로 섹션: 카피 문구 + CTA 버튼 2개(`안심 코스 둘러보기` → `/courses`, `동반 모드 체험` → `/companion`)
- 통계 3개 노출(18 시군 / 30 코스 / 100+ 터널) — **하드코딩된 텍스트**, 실제 COURSES/TUNNELS 배열 길이와 무관
- 우측에 `GangwonMap` 배치: 시군 클릭 시 `/courses?region={name}` 이동
- 기능 카드 4개(그리드): 안심 코스 큐레이션 / 터널 정보 백과 / 안심 경로 길찾기 / 터널 동반 모드 — 각각 해당 라우트로 이동

### 8-2. `CoursesPage.jsx`
- `useSearchParams`로 `region` 쿼리 초기값 읽어 지역 필터 상태(`selected`)로 사용
- 좌측(sticky): `GangwonMap`(지역 선택/해제 토글) + 등급 3단계 범례
- 우측: 카테고리 필터 버튼(`전체/자연/해안` — `COURSES[].tags` 값과 매칭), 코스 카드 리스트
  - 필터링은 전부 클라이언트 사이드 `.filter()`로 처리 (region은 `region.includes()` 부분일치, tag는 `tags.includes()` 완전일치)
- 코스 카드 클릭 → `/courses/:id`

### 8-3. `CourseDetailPage.jsx`
- `useParams()`로 코스 id 조회, 없으면 "코스를 찾을 수 없어요" 렌더
- 좌측: 등급 배지, 태그, 거리, 요약, **경유지(spots) 타임라인 리스트** (번호 원 + 타입별 색상 박스 + 이름/타입/설명)
- 우측(sticky): **"지도(카카오맵 연동 예정)"라고 명시된 placeholder 박스** — 실제 지도 미구현 상태를 코드에서 이미 인지하고 있음
- 하단 CTA: `이 코스로 안심 길찾기` → `/route`로 `{ dest: course.region }` state 전달

### 8-4. `TunnelsPage.jsx`
- 검색창: 이름/도로명 부분일치 검색 (클라이언트 필터)
- 정렬 필터: 전체 / 난이도 높은순 / 낮은순
- `DiffBar` 내부 컴포넌트: 1~5칸 바 그래프로 난이도 시각화
- 터널 카드: 길이(km 환산), 차로, 환기, 정체 4개 스펙 그리드 노출
- 카드 클릭 → `/tunnels/:id`

### 8-5. `TunnelDetailPage.jsx`
- 난이도별 설명 텍스트(`DIFF_META`, 1~5단계 고정 문구) 노출
- **평균 통과 시간**을 `lengthM / 80(km/h) * 3.6`으로 클라이언트에서 즉석 계산(고정 속도 가정 — 실제 교통 데이터 미반영)
- 스펙 6개(길이/차로수/환기등급/평균통과/정체빈도/공황난이도) 그리드

### 8-6. `RoutePage.jsx` — 3단계 위저드 (`step` state: `input` → `compare` → `detail`)
- **input**: 출발지/목적지 텍스트 입력, 최근 검색 3건(`RECENT`, 하드코딩) 클릭 시 자동 채움, `검색` 클릭 시 700ms `setTimeout`으로 로딩 흉내 후 다음 단계
- **compare**: 두 경로 카드 비교
  - "터널 회피 루트"(추천, 초록) vs "최단 루트"(터널 4개, 빨강)
  - 표시 수치는 전부 `MOCK_RESULT` **고정 객체** (실제 경로 계산 없음)
- **detail**: 선택한 경로의 SVG 가짜 곡선 지도(`<path d="M70 360 C ...">`, 실제 지도 좌표 아님), 소요시간/거리/터널수 요약, 회피 루트면 안심 안내 배너, 최단 루트면 경고 배너 + 주요 경유지 리스트
  - `길안내 시작` → `/navigating`으로 `{ origin, dest, durationMin, distanceKm, tunnel? }` 전달 (회피 루트는 `tunnel` 없음)

### 8-7. `NavigatingPage.jsx`
- 전체화면, `location.state`에서 여정 정보 수신 (없으면 기본값 사용)
- `setInterval`로 500ms마다 진행률 +5%p 증가시켜 **가짜 실시간 주행 애니메이션** 구현 (실제 GPS/위치 연동 없음)
- 진행률 55% 시점에 `tunnel`이 존재하면 하단에 "터널 접근" 알림 카드 표시 → `동반 시작`(→ `/companion`) 또는 `나중에`(dismiss) 선택
- 100% 도달 시 "도착 완료" 화면으로 전환

### 8-8. `CompanionPage.jsx` — CBT 호흡 가이드 + 터널 통과 시뮬레이션
- 4단계 호흡 사이클(`PHASES`): 들이쉬기 4초 → 멈춤 2초 → 내쉬기 6초 → 쉬기 1초, `setInterval` 1초 간격으로 순환(`useRef`로 최신 phase/count 추적해 클로저 문제 회피)
- 진행률(`pct`)도 별도 `setInterval`로 30초 동안 0→100% 증가하는 **가짜 시뮬레이션** (`tunnel.lengthM` 기준 잔여 거리 표시)
- 3가지 호흡 시각화 모드 선택 가능: 물결 파동(ripple) / 음파(wave) / 차오름(tide) — 전부 CSS 애니메이션 + 인라인 SVG
- 상단에 "보호자 실시간 위치 공유 중" 인디케이터 상시 노출(pulse 애니메이션, 실제 전송 없음)
- 완료 화면: 통과 시간/누적 통과 횟수(하드코딩: 3:14, 7회), "보호자에게 통과 완료 알림을 보냈어요" 문구(실제 발송 없음)

### 8-9. `MyLoginPage.jsx`

**`LoginPage`**
- 이름 텍스트 입력만으로 로그인 처리(`nav('/home')`), 실제 인증 로직 없음
- 하단에 "실제 서비스에서는 소셜 로그인이 제공돼요" 안내 문구로 향후 계획 명시

**`MyPage`**
- 프로필 카드(이름 하드코딩 "서연 님") + "프로필 수정" 버튼(동작 없음)
- 이번 달 기록 통계 3개(터널 통과 성공 12회, 안심경로 회피 5회, 평균 난이도 3단계) — **전부 하드코딩**
- 보호자 연동 카드: 보호자 1명(김민준) 표시(하드코딩) + "연락처 변경" 버튼(동작 없음)
- 토글 3개(로컬 `useState`만, 서버 저장 없음): 실시간 위치 공유 / 자동 통과 알림 / 긴급 호출 위임 — `Toggle` 내부 컴포넌트로 구현
- 서비스 메뉴: 이용 내역 / 앱 설정 / 문의하기(모두 클릭 동작 없음), 로그아웃(→ `/login`)

---

## 9. 현재 상태 요약 — "구현된 것" vs "만들어야 할 것"

| 구현됨 (프론트 화면 흐름) | 미구현 (실제 동작) |
|---|---|
| 전체 페이지 라우팅 및 페이지 간 상태 전달 | 인증/세션 (이름만 입력하면 통과) |
| 강원 18개 시군 SVG 지도 + 등급 시각화 | 지역 등급/터널 난이도 산정 로직 (값은 모두 수기 입력) |
| 코스/터널 목록·검색·필터·정렬 UI | 실제 데이터 API (전부 `mock.js` 배열) |
| 경로 비교/길안내 3단계 위저드 UI | 실제 경로 탐색 엔진 연동 (결과가 `MOCK_RESULT` 고정값) |
| 실시간 주행/터널 통과 진행률 애니메이션 | 실제 GPS 위치 추적 (`setInterval` 가짜 증가) |
| CBT 호흡 가이드 타이머 + 3종 시각화 | 통과 세션 기록 저장, 보호자 실제 알림 발송 |
| 마이페이지 UI(통계/보호자/설정 토글) | 통계 집계, 보호자 CRUD, 설정 서버 저장 |
| "카카오맵 연동 예정" placeholder 노출 | 실제 지도 SDK 연동 |

---

## 10. 백엔드에서 수행해야 할 과업 / 필요 API

### A. 인증·사용자 — **회원가입/로그인/내정보/전화번호 인증 백엔드 구현 완료**

| API | 상태 | 설명 |
|---|---|---|
| `POST /api/members/signup` | ✅ 완료 | 회원가입 (아이디/비밀번호/이름/전화/이메일 + 보호자 1인 정보). 전화번호 중복가입도 막음(`DuplicatePhoneException`, DB unique 제약). SMS 인증코드 검증은 인프라는 있지만 아직 이 호출에 안 걸려있음(원하면 바로 연결 가능) |
| `GET /api/members/check-username` | ✅ 완료 | 아이디 중복확인 |
| `POST /api/auth/login` | ✅ 완료 (단, 아이디/비밀번호 방식) | JWT 발급. LoginPage 안내 문구가 말한 "소셜 로그인"은 별도 과업 — 필요해지면 그때 OAuth2 연동 추가 |
| `GET /api/members/me` | ✅ 완료 | 마이페이지 프로필(이름/연락처/이메일/보호자 정보) 조회 |
| `PATCH /api/members/me` | ✅ 완료 | "프로필 수정" 대응 (이름/전화/보호자만 변경 가능) |
| `POST /api/sms/send` `/verify` | ✅ 완료 | 본인 전화번호 인증(옥토모 MO 문자 인증, 실제 API로 검증 완료) — 회원가입/로그인과는 별개 흐름으로 이미 동작하지만, 회원가입 절차에 "필수"로 강제하려면 `signup()`에 한 줄 연결 필요 |
| `POST /api/auth/logout` | 불필요 (stateless JWT) | 서버에 세션이 없어 프론트에서 토큰 폐기로 충분 |

### B. 보호자(동반자) 연동 — 전화번호 인증까지는 완료, 실제 연동/알림은 아직

보호자 1인(이름/전화/관계)은 `Member`에 내장되어 있어 `GET/PATCH /api/members/me`로 이미 조회·수정 가능 (`MyPage`의
보호자 카드·"연락처 변경"은 이 두 엔드포인트로 대체 가능, 별도 `/guardians` 리소스는 불필요). 그 보호자 전화번호가
실제로 존재하고 소유자가 응답 가능한지도 확인할 수 있음:

| API | 상태 | 설명 |
|---|---|---|
| `POST /api/sms/guardian/send` `/verify` `/qr` | ✅ 완료 | 보호자 전화번호 인증(로그인 필요) — 문자로 직접, 또는 QR 스캔으로 자동 채워진 문자 전송. **단, 이건 "번호 소유 확인"일 뿐** — 보호자용 로그인 계정이나, 보호자가 실시간 위치/알림을 받는 세션 개념은 아님 |
| `PATCH /users/me/settings` | ❌ 미구현 | 위치 공유·자동 통과 알림·긴급 호출 위임 3개 토글 저장(현재 로컬 state만 존재) |
| `POST /guardian-notifications` (또는 push 연동) | ❌ 미구현 | 동반모드 완료 시 "보호자에게 알림" 실제 발송 — 옥토모는 수신(MO) 전용이라 이 용도로 못 씀, 발송(MT) 채널 별도 필요. 실시간 위치 공유 스트림(WebSocket/SSE)도 미구현 |

### C. 코스·터널·지역 데이터 (현재 `mock.js`를 대체할 CRUD/조회 API)
| API | 설명 |
|---|---|
| `GET /regions` | 18개 시군 + 등급(green/amber/red) + 터널 수 |
| `GET /courses?region=&tag=` | 코스 목록, region/tag 필터 (현재 프론트에서 클라이언트 필터링 중) |
| `GET /courses/{id}` | 코스 상세 + spots(경유지) |
| `GET /tunnels?query=&sort=` | 터널 목록, 이름/도로 검색, 난이도 정렬 |
| `GET /tunnels/{id}` | 터널 상세 스펙 |

등급(GRADE)·난이도(DIFF) 산정 로직도 백엔드(또는 관리자 도구)에서 계산/관리할 대상 — 지금은 하드코딩된 값입니다.

### D. 경로 탐색 (RoutePage) — 가장 핵심 과업
| API | 설명 |
|---|---|
| `POST /routes/search` `{origin, dest}` → `{avoidRoute, shortestRoute}` | 실제 경로 탐색 엔진 연동 필요 (카카오모빌리티/티맵/네이버 등 길찾기 API + 터널 DB 매칭으로 "터널 포함 여부" 판정). 현재 `MOCK_RESULT` 고정값 사용 중 |
| 각 route 응답 필드 | `durationMin, distanceKm, tunnelCount, tunnelNames[], waypoints[], polyline(경로 좌표)` — 지도 표시용 실제 좌표 필요(현재 RoutePage는 SVG로 가짜 곡선만 그림) |
| 지오코딩 | 출발지/목적지 텍스트 → 좌표 변환 (자동완성/최근 검색 포함, `RECENT` 하드코딩 대체) |

### E. 실시간 내비게이션 + 동반 모드 세션
| API | 설명 |
|---|---|
| `POST /navigation/sessions` | 주행 시작 (경로/터널 정보 포함) |
| 위치 업데이트 (WebSocket 또는 주기적 `PATCH`) | 현재 위치→진행률 계산, 터널 접근 감지(현재는 `setInterval`로 진행률을 가짜로 올림) |
| `POST /companion/sessions` | 동반 모드(호흡가이드) 시작 |
| `PATCH /companion/sessions/{id}` (완료 시) | 통과 시간·통과 여부 기록, 보호자 알림 트리거 |
| `GET /companion/sessions/{id}` | 진행 중 위치/진행률 조회(보호자 공유용) |

### F. 통계/기록 (MyPage "이번 달 기록")
| API | 설명 |
|---|---|
| `GET /users/me/stats?period=month` | 터널 통과 성공 횟수, 안심 경로 회피 횟수, 평균 통과 난이도 — 현재 12회/5회/3단계로 하드코딩됨 |
| `GET /users/me/history` | "이용 내역" 메뉴 대응 (연동 안 된 버튼) |

### G. 지도 연동
- CourseDetailPage에 `"지도(카카오맵 연동 예정)"` placeholder가 이미 명시됨 → **카카오맵 SDK/API 연동**이 확정된 과업(경유지 마커 표시)
- RoutePage/NavigatingPage의 SVG 가짜 경로도 실제 지도(폴리라인) 연동 필요

---

**우선순위 제안**: ① 경로탐색 API(터널 회피 로직 포함) → ② 코스/터널/지역 데이터 API화 → ③ 보호자 실제 연동/알림(발송 채널) → ④ 동반모드 세션 기록 및 통계

(인증·회원가입·본인/보호자 전화번호 인증은 이미 백엔드에 구현됨 — A/B장 참고)
