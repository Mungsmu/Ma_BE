# 진행 상황 정리 (2026-09-09 기준)

Spring Boot 기반 백엔드. 회원가입/로그인(JWT)/내 정보 조회·수정/전화번호 인증(옥토모 MO 문자 인증)이
`auth`/`user`/`common` 기능별 패키지로 구성돼 있다. 프론트엔드(Thymeleaf 뷰)는 제거했고, 이 저장소는 API 전용
백엔드로만 간다. 별도 프론트엔드(React, `PROJECT_OVERVIEW.md` 참고)가 이 API를 소비할 예정.

## 1. 기술 스택

- **Spring Boot 4.1.0** (Java 17 toolchain)
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- **JJWT 0.12.6** (`jjwt-api`/`jjwt-impl`/`jjwt-jackson`) — 자체 로그인 API에서 HS256 토큰 발급/검증
- **옥토모(Octomo) API** — MO(수신) 문자 인증 서비스, `RestClient`로 직접 연동(별도 SDK 없음)
- **H2 인메모리 DB** (데모용, 재시작 시 데이터 소실)
- Gradle (Groovy DSL), 별도 린터/포매터 없음

## 2. 모듈(패키지) 구조

기능 단위로 3개 패키지(`auth`, `user`, `common`)로 나눠져 있다 (Gradle 멀티모듈은 아니고, 단일 모듈 내 패키지 분리).

```
com.example.demo
├── DemoApplication.java  (@EnableScheduling — SMS 코드 정리 배치용)
├── auth/                 인증/보안 인프라
│   ├── config/           SecurityConfig, PasswordConfig
│   ├── service/          MemberUserDetailsService
│   ├── controller/       AuthController (로그인)
│   ├── dto/              LoginRequest
│   ├── jwt/              JwtTokenProvider, JwtAuthenticationFilter
│   └── sms/               옥토모 MO 문자 인증 (SmsController, OctomoClient,
│                          SmsVerificationService, SmsNotVerifiedException, dto/)
├── user/                 회원 도메인
│   ├── controller/       MemberController
│   ├── service/          MemberService, DuplicateUsernameException, DuplicatePhoneException
│   ├── domain/           Member, Guardian
│   ├── repository/       MemberRepository
│   └── dto/              SignupRequest, MemberResponse, UpdateMemberRequest
└── common/               공용 인프라
    ├── dto/              ApiResponse
    └── exception/        GlobalExceptionHandler
```

## 3. REST API (`/api/**`)

`AuthController`

- `POST /api/auth/login` — 아이디/비밀번호 검증 후 JWT 발급 (`{accessToken, tokenType: "Bearer"}`)

`MemberController`

- `GET /api/members/check-username?username=` — 아이디 사용 가능 여부 확인 (공개)
- `POST /api/members/signup` — 회원가입 (공개, `SignupRequest` bean validation 통과 필요)
- `GET /api/members/me` — 내 정보 조회 (**로그인 필요**)
- `PATCH /api/members/me` — 내 정보 수정 (**로그인 필요**, 이름/전화번호/보호자 정보만 변경 가능 — 아이디/이메일/비밀번호는 요청 DTO에 필드 자체가 없어서 변경 불가)

`SmsController` — 옥토모(Octomo) MO 문자 인증. 서버가 문자를 발송하는 게 아니라, 코드를 화면에 보여주고
사용자가 그 코드를 옥토모 대표번호(1666-3538)로 직접 문자로 보내면, 그 수신 여부를 옥토모 API로 조회하는 방식.

- `POST /api/sms/send` — 본인 전화번호 인증코드 발급 (공개, `{code, text}` 반환 — `text`가 실제로 보내야 하는 프리픽스 포함 문자열 `u-{code}`)
- `POST /api/sms/verify` — 본인 전화번호 인증 확인 (공개, 소비하지 않음 — `matches()`)
- `POST /api/sms/guardian/send` — 보호자 전화번호 인증코드 발급 (**로그인 필요**, `g-{code}` 프리픽스)
- `POST /api/sms/guardian/verify` — 보호자 전화번호 인증 확인 (**로그인 필요**, 성공 시 소비 — `verifyAndConsume()`)
- `POST /api/sms/guardian/qr` — 발급된 보호자 인증코드를 SMS QR(스캔하면 수신번호·본문 자동 채워짐)로 발급 (**로그인 필요**, `guardian/send`로 코드 먼저 발급해야 함)

모든 API 응답은 공통 포맷 `ApiResponse<T>(success, message, data)`로 통일.

### 인증 방식: JWT (stateless)

- `POST /api/auth/login`이 `AuthenticationManager`로 아이디/비밀번호를 검증(기존 `MemberUserDetailsService` +
  `PasswordConfig`의 `PasswordEncoder`가 이미 있어서 `DaoAuthenticationProvider`가 자동 구성됨)하고, 성공하면
  `JwtTokenProvider`가 HS256으로 서명한 토큰을 발급한다. 시크릿/만료시간은 `app.jwt.secret`/`app.jwt.expiration-ms`
  (기본 1시간)로 `application.properties`에 있음 — **개발용 시크릿이 여전히 코드에 인라인**돼 있으니 운영 배포 전
  반드시 환경변수로 교체할 것(옥토모 키처럼 `.env`/`${VAR}` 방식으로 뺄 수 있음, 아직 안 함).
- 이후 요청은 `Authorization: Bearer <token>` 헤더로 인증한다. `JwtAuthenticationFilter`가 매 요청마다 토큰을
  검증해서 유효하면 `SecurityContext`에 인증 정보를 채워 넣는다.
- 세션/쿠키를 쓰지 않으므로(`SessionCreationPolicy.STATELESS`) CSRF도 비활성화했다.
- `permitAll` 대상은 화이트리스트로 명시: `/api/auth/login`, `/api/members/signup`, `/api/members/check-username`,
  `/api/sms/send`, `/api/sms/verify`. 그 외 전부(`anyRequest().authenticated()`) 토큰이 있어야 접근 가능 —
  `/api/sms/guardian/**`도 여기 포함(의도적으로 공개 안 함, 보호자 인증은 로그인된 회원만 요청 가능해야 해서).
- 토큰이 없거나 무효하면 Spring Security 기본값인 403 대신 **401**을 반환하도록 커스텀 `authenticationEntryPoint`를
  달아뒀다.
- 로그인 실패(아이디 없음/비밀번호 불일치)는 `GlobalExceptionHandler`의 `AuthenticationException` 핸들러가 401로
  매핑.
- 로그아웃은 별도 API 없음 — stateless라 서버가 세션을 들고 있지 않으므로, 클라이언트가 토큰을 버리는 것으로 충분.

## 4. 회원가입 검증 로직

`SignupRequest`에서 1차 검증(Bean Validation): 이름/전화번호(휴대폰 정규식)/이메일/아이디(4~20자 영문·숫자·`_`)/비밀번호(영문+숫자 8자 이상)/비밀번호 확인/보호자 이름·전화번호·관계 모두 필수.

`MemberService.signup()`에서 2차 검증:
1. 비밀번호 == 비밀번호 확인 (`IllegalArgumentException`)
2. 아이디 중복 (`DuplicateUsernameException` → HTTP 409)
3. 전화번호 중복 (`DuplicatePhoneException` → HTTP 409, `Member.phone`에 DB unique 제약도 걸려있음)
4. 통과 시 `Guardian` + `Member` 생성 후 저장

⚠️ SMS 인증코드 검증은 **아직 여기서 호출 안 함** — 5장 참고.

## 5. 옥토모(Octomo) MO 문자 인증 — 실제 연동 완료, 회원가입과는 아직 미연동 ⚠️

옥토모는 발신(MT) 서비스가 아니라 **수신(MO) 조회 서비스**다 — "옥토모 대표번호(1666-3538)로 도착한 문자를
조회"하는 API만 제공한다. 그래서 이 서비스는 우리가 사용자에게 문자를 보내는 게 아니라, 코드를 화면에 보여주고
사용자가 그 코드를 직접 옥토모 번호로 전송하면, 서버가 그 수신 여부를 조회해서 인증을 완료하는 구조다.

- `OctomoClient` — `POST /octomo/v1/public/message/exists`(수신 조회), `POST /octomo/v1/public/message/qr-code`
  (SMS QR 발급, `SMSTO:{수신번호}:{text}` 딥링크를 옥토모가 PNG QR로 변환해줌) 2개 엔드포인트만 감싼 얇은 클라이언트.
  API Key는 `.env`(gitignore됨, `.env.example`이 템플릿)의 `OCTOMO_API_KEY` → `spring.config.import`로 로드 →
  `app.octomo.api-key=${OCTOMO_API_KEY:}`로 주입. **실제 옥토모 서버로 curl 수동 검증 완료**(본인 인증 성공까지 확인).
- `SmsVerificationService` — 용도별 프리픽스(`Purpose.USER`→`u-`, `Purpose.GUARDIAN`→`g-`)를 붙여 옥토모에
  조회한다. 코드는 인메모리 `ConcurrentHashMap`에 저장(운영 시 Redis 권장 — 재시작하면 날아가고, 서버를 여러 대로
  스케일하면 서버 간 공유가 안 됨). `issueCode()`가 코드를 생성해 반환(발송 없음), `matches()`는 소비 안 하고 확인만,
  `verifyAndConsume()`은 성공 시 `matched=true`로 표시(삭제 대신 보관 — 재사용 방지 + 감사 기록).
  - `app.sms.code-ttl-seconds`(기본 180초) — 코드 자체의 유효시간
  - `app.sms.code-lookup-window-minutes`(기본 30) — 이보다 오래된 코드는 옥토모 API를 부르지 않고 로컬에서 즉시
    실패 처리(불필요한 네트워크 호출 방지). TTL보다 큰 값이라 실제로는 TTL이 먼저 걸림 — 이중 안전장치.
  - `app.sms.code-retention-hours`(기본 24) — `@Scheduled` `purgeStaleCodes()`가 매칭 완료/미매칭/만료 여부와
    무관하게 이 시간이 지난 코드를 저장소에서 물리적으로 삭제.
- **미완성 부분**: `verifyAndConsume()`(인증 후 소비)이 `MemberService.signup()`에서 호출되지 않음. `SignupRequest`에도
  인증코드 필드가 없어, 현재 회원가입은 SMS 인증 없이도 완료된다. `SmsNotVerifiedException`도 정의는 되어 있으나
  (예외 핸들러 등록됨) 실제로 던져지는 곳이 없음.
- **보호자 인증의 의미**: `/api/sms/guardian/*`는 "보호자 전화번호가 실재하고 그 번호 소유자가 문자를 보낼 수 있음"만
  확인한다 — 보호자용 로그인 계정이나 별도 세션을 만드는 기능은 아니다(그런 게 필요하면 별도 설계 필요).
- **실제 연동 확인 과정에서 잡은 버그 2개** (둘 다 유닛 테스트로는 못 잡는 종류, 실제 API 호출로만 발견됨):
  1. `/api/sms/send` 응답이 프리픽스 없는 코드만 줘서 사용자가 뭘 보내야 할지 알 수 없었던 문제 → 응답에 `text`
     필드(프리픽스 포함) 추가.
  2. 옥토모 `mobileNum`은 하이픈 없는 11자리 숫자를 요구하는데 하이픈 붙은 원본 번호를 그대로 넘겨서 매번
     `400 Bad Request`였던 문제 → 정규화된 번호로 수정.

## 6. 공통 예외 처리 (`GlobalExceptionHandler`)

`/api/**` 요청에 한해 `@RestControllerAdvice`로 예외 → `ApiResponse.fail(...)` JSON 변환:

| 예외 | HTTP 상태 |
|---|---|
| `MethodArgumentNotValidException` (`@RequestBody` 검증 실패) | 400 |
| `ConstraintViolationException` (`@RequestParam` 등 검증 실패) | 400 |
| `DuplicateUsernameException` | 409 |
| `DuplicatePhoneException` | 409 |
| `SmsNotVerifiedException` | 400 |
| `IllegalArgumentException` | 400 |

## 7. DB / 엔티티 현황

`Member`(회원)와 `Guardian`(보호자, `@Embeddable`로 `member` 테이블에 내장) 2개 엔티티. H2 인메모리 DB +
`spring.jpa.hibernate.ddl-auto=update`로 애플리케이션 기동 시 테이블이 자동 생성된다. DB 자체(스키마 마이그레이션
도구 등)는 없고, 엔티티 정의가 곧 스키마다.

### 실제 스키마 (엔티티 기준)

```sql
CREATE TABLE member (
    id                BIGINT       GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username          VARCHAR(30)  NOT NULL UNIQUE,   -- 로그인 아이디
    password          VARCHAR(255) NOT NULL,          -- BCrypt 해시
    name              VARCHAR(50)  NOT NULL,
    phone             VARCHAR(20)  NOT NULL UNIQUE,   -- SMS 인증 대상, 중복 가입 방지
    email             VARCHAR(100) NOT NULL,
    guardian_name     VARCHAR(50),                    -- Guardian embedded
    guardian_phone    VARCHAR(20),                     -- Guardian embedded
    guardian_relation VARCHAR(20),                     -- Guardian embedded
    created_at        TIMESTAMP
);
```

- `Guardian`은 별도 테이블이 아니라 `@Embedded`로 `member` 테이블에 컬럼 3개(`guardian_*`)로 펼쳐진다 — 회원과 1:1로 항상 같이 존재.
- `email`/`guardian_*` 컬럼에는 DB 레벨 unique 제약이 없다. `email` 중복 가입 가능 상태 (전화번호는 이번에 unique 추가됨, 이메일은 아직).
- FK나 별도 연관관계 테이블 없음 — 회원-보호자가 1:1 내장 관계라 필요 없음.

## 8. 테스트 / 문서

- `DemoApplicationTests` — 컨텍스트 로드 확인 1건.
- `SmsVerificationServiceTest` — `OctomoClient`를 mock으로 대체한 유닛 테스트 6건(매칭 성공/실패, 용도 불일치,
  재사용(replay) 차단, 미발급 코드 조기 실패, 보호자 QR 발급 가드).
- `CLAUDE.md`에 아키텍처와 개발 명령어 문서화됨. `PROJECT_OVERVIEW.md`는 별도 React 프론트엔드의 구조 문서.

## 다음에 이어서 할 만한 작업 (미정, 참고용)

- 회원가입 시 SMS 인증 실제 연동 (`verifyAndConsume` 호출 + `SignupRequest`에 인증코드 필드 추가) — 인프라는 다 있고 연결만 안 됨
- `app.jwt.secret`을 `.env`/환경변수로 분리 (옥토모 키는 이미 이 패턴으로 뺐음, JWT 시크릿은 아직 하드코딩)
- SMS 인증코드 저장소를 Redis로 (지금은 인메모리라 재시작 시 소실, 다중 인스턴스 스케일 시 서버 간 공유 안 됨)
- 보호자 실제 연동/알림 설계 — 지금 `/api/sms/guardian/*`는 "전화번호 소유 확인"까지만, 보호자가 실제로 위치를
  보거나 알림을 받는 세션/계정 개념은 미설계
- 보호자에게 실제 SMS 알림을 보내는 "길찾기" 기능 자체 (아직 존재하지 않음) — 옥토모는 수신 전용이라 이 용도로는
  못 씀, 별도 발송 채널 필요
- 서비스/컨트롤러 단위 테스트 추가 (JWT 로그인/내 정보 API, MemberService 등은 여전히 curl 수동 검증만 함)
- 리프레시 토큰 / 토큰 만료 시 재발급 흐름 (지금은 액세스 토큰 1개, 만료되면 재로그인만 가능)
- 패키지명 `com.example.demo` → 프로젝트에 맞는 이름으로 정리
- `PROJECT_OVERVIEW.md` 10장(코스/터널/지역 데이터, 경로 탐색, 실시간 내비게이션/동반 모드, 통계, 지도 연동)에
  해당하는 기능은 이 저장소에 아직 전혀 없음 — 가장 큰 미착수 영역
