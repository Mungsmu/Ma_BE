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

## Architecture

Package-by-feature, not package-by-layer: three top-level packages, each self-contained with its own `controller`/`service`/`domain`/`repository`/`dto` subpackages as needed. This is a single Gradle module — not a multi-module build — just organized by feature.

- **`auth`** — security infrastructure: `SecurityConfig`, `PasswordConfig`, `MemberUserDetailsService`, `AuthController` (login), the `jwt` subpackage (`JwtTokenProvider`, `JwtAuthenticationFilter`), and the `sms` subpackage (SMS phone-verification: `SmsController`, `SmsSender` interface + `ConsoleSmsSender` demo impl, `SmsVerificationService`).
- **`user`** — member domain: `MemberController` (signup/username-check/my-info), `MemberService`, `Member`/`Guardian` entities, `MemberRepository`, DTOs.
- **`common`** — cross-cutting: `ApiResponse<T>` (the response envelope every `/api/**` endpoint returns) and `GlobalExceptionHandler`.

Cross-package dependencies are expected and fine (e.g. `auth.service.MemberUserDetailsService` depends on `user.repository.MemberRepository`) — don't try to eliminate them by merging packages.

- **Auth is JWT, stateless**: `POST /api/auth/login` (`AuthController`) authenticates via `AuthenticationManager` (auto-wired from the existing `MemberUserDetailsService` + `PasswordConfig` beans, no extra provider code) and returns a signed HS256 token (`JwtTokenProvider`, secret/expiry from `app.jwt.secret`/`app.jwt.expiration-ms`). `JwtAuthenticationFilter` reads `Authorization: Bearer <token>` on every request and populates `SecurityContext` if valid; it never blocks by itself — `SecurityConfig`'s `authorizeHttpRequests` does the actual gating. No sessions, no CSRF (`sessionCreationPolicy(STATELESS)` + `csrf.disable()`), so the old `webSecurityCustomizer().ignoring("/api/**")` workaround (needed only because CSRF blocked POSTs under session auth) is gone — `webSecurityCustomizer` now only ignores `/h2-console/**`. A custom `authenticationEntryPoint` returns 401 (not Spring's default 403) for missing/invalid tokens.
- **Public (permitAll) endpoints** are an explicit allowlist in `SecurityConfig`: `/api/auth/login`, `/api/members/signup`, `/api/members/check-username`, `/api/sms/**`. Everything else — including `GET`/`PATCH /api/members/me` — requires a valid JWT via `anyRequest().authenticated()`. Any new endpoint is authenticated-by-default; add it to the allowlist explicitly if it should be public.
- **`PasswordConfig` is split out from `SecurityConfig`** specifically to avoid a circular dependency with the `UserDetailsService` bean — don't merge them back.
- **Signup flow**: `SignupRequest` (bean-validated record) → `MemberService.signup()` checks password confirmation match, then username uniqueness (`DuplicateUsernameException`), then builds an embedded `Guardian` (name/phone/relation, used later for SMS alerts) and creates a `Member` via its static factory (constructor is private, no-arg ctor is JPA-only). Password is BCrypt-hashed in the service, never in the entity.
- **Profile update is allowlist-by-omission**: `UpdateMemberRequest` (used by `PATCH /api/members/me`) simply has no `username`/`email` fields, so there's no way to smuggle a change to them through that endpoint — no extra "immutable field" validation code needed. `Member.updateProfile(name, phone, guardian)` is the only mutation path (no public setters); password change isn't implemented.
- **SMS verification is a separate, not-yet-wired subsystem**: `auth.sms.SmsController` (`/api/sms/send`, `/api/sms/verify`) drives `SmsVerificationService`, which stores one-time codes in an in-memory `ConcurrentHashMap` keyed by normalized phone number (hyphens stripped), with TTL from `app.sms.code-ttl-seconds` (default 180s). `matches()` checks without consuming; `verifyAndConsume()` checks-and-removes and is intended for final signup consumption, but `MemberService.signup()` does not currently call it — `SignupRequest.verificationCode` doesn't exist yet either. `SmsSender` is an interface so the demo `ConsoleSmsSender` (logs instead of sending) can be swapped for a real provider (CoolSMS/NHN Cloud/Twilio) without touching `SmsVerificationService`.
- **Errors are centralized** in `common.exception.GlobalExceptionHandler` (`@RestControllerAdvice`): validation errors (`MethodArgumentNotValidException`, `ConstraintViolationException`), `DuplicateUsernameException`, `SmsNotVerifiedException`, and generic `IllegalArgumentException` all map to `ApiResponse.fail(...)` with an appropriate HTTP status.
- **Persistence**: H2 in-memory (`spring.jpa.hibernate.ddl-auto=update`), console at `/h2-console`. Data does not survive restarts — this is demo/dev config, not production-ready. Entity definitions ARE the schema (no migration tool); see `PROGRESS.md` for the current derived DDL.
