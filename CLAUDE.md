# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1 (Java 17) **API-only backend** for "maeumsumgil" — member signup/login with SMS phone verification infrastructure. No view layer; this repo serves JSON only, a separate frontend consumes it. Package root: `com.example.demo` (not yet renamed to match the project).

## Commands

```bash
./gradlew build              # compile + run tests
./gradlew test                # run all tests
./gradlew test --tests "com.example.demo.DemoApplicationTests"   # single test class
./gradlew bootRun             # run the app (http://localhost:8080)
```

On Windows use `gradlew.bat` instead of `./gradlew`.

There is no linter/formatter configured.

Local secrets (`OCTOMO_API_KEY`) go in a root `.env` (see `.env.example`) — `application.properties` loads it via `spring.config.import=optional:file:.env[.properties]`, no extra library needed.

## Architecture

Package-by-feature, not package-by-layer: three top-level packages, each self-contained with its own `controller`/`service`/`domain`/`repository`/`dto` subpackages as needed. This is a single Gradle module — not a multi-module build — just organized by feature.

- **`auth`** — security infrastructure: `SecurityConfig`, `PasswordConfig`, `MemberUserDetailsService`, `AuthController` (login), the `jwt` subpackage (`JwtTokenProvider`, `JwtAuthenticationFilter`), and the `sms` subpackage (Octomo MO 문자 인증: `SmsController`, `OctomoClient`, `SmsVerificationService`).
- **`user`** — member domain: `MemberController` (signup/username-check/my-info), `MemberService`, `Member`/`Guardian` entities, `MemberRepository`, DTOs.
- **`common`** — cross-cutting: `ApiResponse<T>` (the response envelope every `/api/**` endpoint returns) and `GlobalExceptionHandler`.

Cross-package dependencies are expected and fine (e.g. `auth.service.MemberUserDetailsService` depends on `user.repository.MemberRepository`) — don't try to eliminate them by merging packages.

- **Auth is JWT, stateless**: `POST /api/auth/login` (`AuthController`) authenticates via `AuthenticationManager` (auto-wired from the existing `MemberUserDetailsService` + `PasswordConfig` beans, no extra provider code) and returns a signed HS256 token (`JwtTokenProvider`, secret/expiry from `app.jwt.secret`/`app.jwt.expiration-ms`). `JwtAuthenticationFilter` reads `Authorization: Bearer <token>` on every request and populates `SecurityContext` if valid; it never blocks by itself — `SecurityConfig`'s `authorizeHttpRequests` does the actual gating. No sessions, no CSRF (`sessionCreationPolicy(STATELESS)` + `csrf.disable()`), so the old `webSecurityCustomizer().ignoring("/api/**")` workaround (needed only because CSRF blocked POSTs under session auth) is gone — `webSecurityCustomizer` now only ignores `/h2-console/**`. A custom `authenticationEntryPoint` returns 401 (not Spring's default 403) for missing/invalid tokens.
- **Public (permitAll) endpoints** are an explicit allowlist in `SecurityConfig`: `/api/auth/login`, `/api/members/signup`, `/api/members/check-username`, `/api/sms/send`, `/api/sms/verify`. Everything else — including `GET`/`PATCH /api/members/me` and all `/api/sms/guardian/**` — requires a valid JWT via `anyRequest().authenticated()`. Any new endpoint is authenticated-by-default; add it to the allowlist explicitly if it should be public.
- **`PasswordConfig` is split out from `SecurityConfig`** specifically to avoid a circular dependency with the `UserDetailsService` bean — don't merge them back.
- **Signup flow**: `SignupRequest` (bean-validated record) → `MemberService.signup()` checks password confirmation match, then username uniqueness (`DuplicateUsernameException`) and phone uniqueness (`DuplicatePhoneException` — `Member.phone` is `unique = true`), then builds an embedded `Guardian` (name/phone/relation, used later for SMS alerts) and creates a `Member` via its static factory (constructor is private, no-arg ctor is JPA-only). Password is BCrypt-hashed in the service, never in the entity.
- **Profile update is allowlist-by-omission**: `UpdateMemberRequest` (used by `PATCH /api/members/me`) simply has no `username`/`email` fields, so there's no way to smuggle a change to them through that endpoint — no extra "immutable field" validation code needed. `Member.updateProfile(name, phone, guardian)` is the only mutation path (no public setters); password change isn't implemented.
- **SMS verification uses Octomo, an MO (inbound) lookup service, not a sender**: there is no outbound SMS API — `auth.sms.OctomoClient` calls Octomo's `POST /octomo/v1/public/message/exists` to check whether a code was texted *to* Octomo's number (1666-3538) *from* the user's phone, and `POST /octomo/v1/public/message/qr-code` to get a QR that pre-fills that text so the user only has to tap send. `SmsVerificationService.issueCode(phone, Purpose)` generates a code and returns it directly (nothing is sent — the code is just displayed in the UI for the user to text back), prefixed by purpose when checked against Octomo (`Purpose.USER` → `u-`, `Purpose.GUARDIAN` → `g-`). Codes live in an in-memory `ConcurrentHashMap` keyed by normalized phone; `findValidEntry()` rejects a lookup locally (no Octomo call) once past `app.sms.code-ttl-seconds` (default 180s) or `app.sms.code-lookup-window-minutes` (default 30) — that 30-minute knob exists purely to skip the network round-trip for obviously-stale codes, it doesn't extend validity. A separate `@Scheduled` `purgeStaleCodes()` (needs `@EnableScheduling` on `DemoApplication`) physically deletes entries — matched, unmatched, or expired — older than `app.sms.code-retention-hours` (default 24). `matches()` checks without consuming; `verifyAndConsume()` checks-and-marks-matched (kept, not removed, so replay is blocked via the `matched` flag rather than absence from the map) and is intended for final signup consumption, but `MemberService.signup()` does not currently call it — `SignupRequest.verificationCode` doesn't exist yet either. Guardian endpoints (`/api/sms/guardian/send`, `/guardian/verify`, `/guardian/qr`) are deliberately left off `SecurityConfig`'s permitAll list — they require a logged-in member, since guardian verification is only meaningful for an already-authenticated account. Octomo API key goes in `app.octomo.api-key` (empty by default).
- **Dev-only SMS bypass**: `app.sms.dev-bypass-phone` (env `SMS_DEV_BYPASS_PHONE`, empty by default) names one normalized phone number that `SmsVerificationService.check()` passes for *any* purpose without hitting Octomo or requiring a code to have been issued — for testing signup/guardian flows without texting Octomo's number. Must stay empty outside local dev; it's a single global value, not per-account.
- **Errors are centralized** in `common.exception.GlobalExceptionHandler` (`@RestControllerAdvice`): validation errors (`MethodArgumentNotValidException`, `ConstraintViolationException`), `DuplicateUsernameException`, `DuplicatePhoneException`, `SmsNotVerifiedException`, and generic `IllegalArgumentException` all map to `ApiResponse.fail(...)` with an appropriate HTTP status.
- **Persistence**: H2 in-memory (`spring.jpa.hibernate.ddl-auto=update`), console at `/h2-console`. Data does not survive restarts — this is demo/dev config, not production-ready. Entity definitions ARE the schema (no migration tool); see `PROGRESS.md` for the current derived DDL.

## Frontend context

`BACKEND_HANDOFF.md` is the current status doc from the frontend team (web + Expo mobile) — it says outright that `PROJECT_OVERVIEW.md` is stale/pre-mock-data and to prefer this file. Per `BACKEND_HANDOFF.md`: only 5 auth endpoints (`check-username`, `sms/send`, `sms/verify`, `signup`, `login`, `members/me`) are wired up on web, and none on mobile yet; everything else (guardian notification SMS, mypage stats, course/tunnel/region data, route search) is still frontend-local/hardcoded. Guardian notification SMS is called out as the highest-priority gap — the frontend already tells users it happens. Check both docs before adding new API surface, trusting `BACKEND_HANDOFF.md` where they disagree.
