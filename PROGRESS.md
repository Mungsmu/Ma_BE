# 진행 상황 정리 (2026-08-10 기준)

Spring Boot 기반 백엔드. 회원가입/로그인(JWT)/내 정보 조회·수정/SMS 인증 인프라가 `auth`/`user`/`common` 기능별
패키지로 구성돼 있다. 프론트엔드(Thymeleaf 뷰)는 제거했고, 이 저장소는 API 전용 백엔드로만 간다.

## 1. 기술 스택

- **Spring Boot 4.1.0** (Java 17 toolchain)
- Spring Web, Spring Data JPA, Spring Security, Bean Validation
- **JJWT 0.12.6** (`jjwt-api`/`jjwt-impl`/`jjwt-jackson`) — 자체 로그인 API에서 HS256 토큰 발급/검증
- **H2 인메모리 DB** (데모용, 재시작 시 데이터 소실)
- Gradle (Groovy DSL), 별도 린터/포매터 없음

## 2. 모듈(패키지) 구조

기능 단위로 3개 패키지(`auth`, `user`, `common`)로 나눠져 있다 (Gradle 멀티모듈은 아니고, 단일 모듈 내 패키지 분리).

```
com.example.demo
├── DemoApplication.java
├── auth/                 인증/보안 인프라
│   ├── config/           SecurityConfig, PasswordConfig
│   ├── service/          MemberUserDetailsService
│   ├── controller/       AuthController (로그인)
│   ├── dto/              LoginRequest
│   ├── jwt/              JwtTokenProvider, JwtAuthenticationFilter
│   └── sms/               SMS 본인인증 (SmsController, SmsSender, ConsoleSmsSender,
│                          SmsVerificationService, SmsNotVerifiedException, dto/)
├── user/                 회원 도메인
│   ├── controller/       MemberController
│   ├── service/          MemberService, DuplicateUsernameException
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

`SmsController`

- `POST /api/sms/send` — 인증번호 발송 (콘솔 로그로 대체 발송, 공개)
- `POST /api/sms/verify` — 인증번호 일치 여부만 확인 (소비하지 않음, 공개)

모든 API 응답은 공통 포맷 `ApiResponse<T>(success, message, data)`로 통일.

### 인증 방식: JWT (stateless)

- `POST /api/auth/login`이 `AuthenticationManager`로 아이디/비밀번호를 검증(기존 `MemberUserDetailsService` +
  `PasswordConfig`의 `PasswordEncoder`가 이미 있어서 `DaoAuthenticationProvider`가 자동 구성됨)하고, 성공하면
  `JwtTokenProvider`가 HS256으로 서명한 토큰을 발급한다. 시크릿/만료시간은 `app.jwt.secret`/`app.jwt.expiration-ms`
  (기본 1시간)로 `application.properties`에 있음 — **개발용 시크릿이 코드에 인라인**돼 있으니 운영 배포 전 반드시
  환경변수로 교체할 것.
- 이후 요청은 `Authorization: Bearer <token>` 헤더로 인증한다. `JwtAuthenticationFilter`가 매 요청마다 토큰을
  검증해서 유효하면 `SecurityContext`에 인증 정보를 채워 넣는다.
- 세션/쿠키를 쓰지 않으므로(`SessionCreationPolicy.STATELESS`) CSRF도 비활성화했다. 예전에 `/api/**` 전체를
  Security 필터 체인에서 우회(`ignoring()`)시켰던 이유(CSRF가 POST를 막던 문제)가 이제 없어져서, 그 워크어라운드는
  제거하고 정상적인 `permitAll`/`authenticated()` 방식으로 바꿨다. `webSecurityCustomizer`는 이제 `/h2-console/**`만
  우회시킨다.
- `permitAll` 대상은 화이트리스트로 명시: `/api/auth/login`, `/api/members/signup`, `/api/members/check-username`,
  `/api/sms/**`. 그 외 전부(`anyRequest().authenticated()`) 토큰이 있어야 접근 가능 — 새 엔드포인트를 추가하면 기본이
  "인증 필요"이므로, 공개로 열어야 한다면 이 화이트리스트에 명시적으로 추가해야 한다.
- 토큰이 없거나 무효하면 Spring Security 기본값인 403 대신 **401**을 반환하도록 커스텀 `authenticationEntryPoint`를
  달아뒀다 (`{"success":false,"message":"인증이 필요합니다.","data":null}`).
- 로그인 실패(아이디 없음/비밀번호 불일치)는 `GlobalExceptionHandler`의 `AuthenticationException` 핸들러가 401로
  매핑.
- 로그아웃은 별도 API 없음 — stateless라 서버가 세션을 들고 있지 않으므로, 클라이언트가 토큰을 버리는 것으로 충분.
  토큰 블랙리스트 같은 서버 측 무효화는 구현 안 함 (필요해지면 그때 추가).
- curl로 직접 검증 완료: 회원가입 → 로그인(토큰 발급) → 토큰 없이 `/me` 호출 시 401 → 토큰으로 `/me` 호출 시 200 →
  `PATCH /me`에 `username`/`email`을 끼워 보내도 무시되고 나머지 필드만 반영되는 것까지 확인함.

## 4. 회원가입 검증 로직

`SignupRequest`에서 1차 검증(Bean Validation): 이름/전화번호(휴대폰 정규식)/이메일/아이디(4~20자 영문·숫자·`_`)/비밀번호(영문+숫자 8자 이상)/비밀번호 확인/보호자 이름·전화번호·관계 모두 필수.

`MemberService.signup()`에서 2차 검증:
1. 비밀번호 == 비밀번호 확인 (`IllegalArgumentException`)
2. 아이디 중복 (`DuplicateUsernameException` → HTTP 409)
3. 통과 시 `Guardian` + `Member` 생성 후 저장

## 5. SMS 인증 — 인프라만 구축, 회원가입과 아직 미연동 ⚠️

- `SmsSender` 인터페이스 + 데모 구현체 `ConsoleSmsSender`(실제 발송 대신 로그만 남김). 추후 CoolSMS/NHN Cloud/Twilio 등으로 교체 가능하도록 추상화.
- `SmsVerificationService`가 인증번호를 인메모리 `ConcurrentHashMap`에 저장(운영 시 Redis 권장, TTL `app.sms.code-ttl-seconds`, 기본 180초). `sendCode`/`matches`/`verifyAndConsume` 3개 메서드 제공.
- **미완성 부분**: `verifyAndConsume()`(인증 후 소비)이 정의만 되어 있고 `MemberService.signup()`에서 호출되지 않음. `SignupRequest`에도 인증코드 필드가 없어, 현재 회원가입은 SMS 인증 없이도 완료된다. `SmsNotVerifiedException`도 정의는 되어 있으나(예외 핸들러 등록됨) 실제로 던져지는 곳이 없음.

## 6. 공통 예외 처리 (`GlobalExceptionHandler`)

`/api/**` 요청에 한해 `@RestControllerAdvice`로 예외 → `ApiResponse.fail(...)` JSON 변환:

| 예외 | HTTP 상태 |
|---|---|
| `MethodArgumentNotValidException` (`@RequestBody` 검증 실패) | 400 |
| `ConstraintViolationException` (`@RequestParam` 등 검증 실패) | 400 |
| `DuplicateUsernameException` | 409 |
| `SmsNotVerifiedException` | 400 |
| `IllegalArgumentException` | 400 |

## 7. DB / 엔티티 현황

**이미 존재함.** `Member`(회원)와 `Guardian`(보호자, `@Embeddable`로 `member` 테이블에 내장) 2개 엔티티가 구현돼 있고,
H2 인메모리 DB + `spring.jpa.hibernate.ddl-auto=update`로 애플리케이션 기동 시 테이블이 자동 생성된다.
DB 자체(스키마 마이그레이션 도구 등)는 없고, 엔티티 정의가 곧 스키마다.

### 실제 스키마 (엔티티 기준)

```sql
CREATE TABLE member (
    id                BIGINT       GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    username          VARCHAR(30)  NOT NULL UNIQUE,   -- 로그인 아이디
    password          VARCHAR(255) NOT NULL,          -- BCrypt 해시
    name              VARCHAR(50)  NOT NULL,
    phone             VARCHAR(20)  NOT NULL,
    email             VARCHAR(100) NOT NULL,
    guardian_name     VARCHAR(50),                    -- Guardian embedded
    guardian_phone    VARCHAR(20),                     -- Guardian embedded
    guardian_relation VARCHAR(20),                     -- Guardian embedded
    created_at        TIMESTAMP
);
```

- `Guardian`은 별도 테이블이 아니라 `@Embedded`로 `member` 테이블에 컬럼 3개(`guardian_*`)로 펼쳐진다 — 회원과 1:1로 항상 같이 존재.
- `email`/`guardian_*` 컬럼에는 DB 레벨 unique 제약이 없다 (username만 unique). `email` 중복 가입 가능 상태.
- FK나 별도 연관관계 테이블 없음 — 회원-보호자가 1:1 내장 관계라 필요 없음.
- 인덱스는 `username`의 unique 제약이 자동으로 잡아주는 것 외에 추가로 없음.

## 8. 테스트 / 문서

- 테스트는 `DemoApplicationTests`의 컨텍스트 로드 확인 1건뿐 — 서비스/컨트롤러 단위 테스트 없음.
- `CLAUDE.md`에 아키텍처와 개발 명령어 문서화됨.

## 다음에 이어서 할 만한 작업 (미정, 참고용)

- 회원가입 시 SMS 인증 실제 연동 (`verifyAndConsume` 호출 + `SignupRequest`에 인증코드 필드 추가)
- 보호자에게 실제 SMS 알림을 보내는 "길찾기" 기능 자체 (아직 존재하지 않음)
- 서비스/컨트롤러 단위 테스트 추가 (현재 JWT 로그인/내 정보 API는 curl 수동 검증만 했고 자동화 테스트 없음)
- `app.jwt.secret`을 환경변수로 분리 (지금은 개발용 시크릿이 `application.properties`에 하드코딩)
- 리프레시 토큰 / 토큰 만료 시 재발급 흐름 (지금은 액세스 토큰 1개, 만료되면 재로그인만 가능)
- 패키지명 `com.example.demo` → 프로젝트에 맞는 이름으로 정리
