# HomeHealthCare Backend

Spring Boot backend for a multi-tenant home healthcare platform with agency isolation, branch-aware authorization, secure session-based authentication, MFA, audit logging, and security-focused account lifecycle management.

This README is written for two audiences:

- backend contributors who need to run and extend the service
- frontend developers who need a clear integration contract to build end-to-end flows on top of the existing backend

## What This Project Already Covers

- multi-tenant data model with `Agency`, `Branch`, `User`, `AgencyMembership`, and `BranchAssignment`
- tenant isolation and branch-aware access control
- email/password login with refresh, logout, lockout protection, and session timeouts
- invite, accept-invite, password reset, password change, and password policy enforcement
- TOTP MFA enrollment and MFA login challenge with recovery codes
- audit events for auth, admin, and security actions
- admin MFA policy and admin notification preferences
- Flyway-managed schema and integration-heavy test coverage

## Tech Stack

- Java 21
- Spring Boot 4
- Spring MVC
- Spring Security
- Spring Data JPA
- Flyway
- H2 in-memory database for local development and tests
- Gradle

## Quick Start

### Prerequisites

- Java 21
- no external database is required for local startup

### Run the app

```bash
./gradlew bootRun
```

The app uses the in-memory H2 database configured in [application.properties](/Users/shiva/Documents/GitHub/homehealthcare/src/main/resources/application.properties), and Flyway migrations run automatically on startup.

### Run tests

```bash
./gradlew test
```

### Run the auth/MFA regression suite only

```bash
./gradlew authMfaTest
```

## Project Structure

```text
src/main/java/com/homehealthcare
  agency/          agency domain and agency MFA policy APIs
  auth/            login, logout, session, password, MFA APIs and services
  branch/          branch domain and branch security rules
  branchassignment/ branch assignment model
  invitation/      invite and accept-invite flows
  membership/      user-to-agency membership model
  notification/    admin notification preferences and critical alert delivery
  platform/audit/  unified audit event model
  platform/email/  email templating and signed link generation
  security/        security config, tenant context, branch scope, authorization
  shared/          persistence base types and tenant-aware repository support
  user/            user model and user-management services

src/main/resources
  application.properties
  db/migration/    Flyway schema history
  emails/          invite, reset, and security alert templates
  security/        password security support data

docs/
  architecture/    ADRs and engineering standards
  reviews/         production-readiness review artifacts
```

## Database and Domain Model

The core Epic 1 schema is created through Flyway migrations in [db/migration](/Users/shiva/Documents/GitHub/homehealthcare/src/main/resources/db/migration).

Main entities:

- `Agency`: top-level tenant
- `Branch`: branch within an agency
- `User`: identity record with status, MFA flag, password hash, profile settings, and last login
- `AgencyMembership`: user membership inside an agency with a role
- `BranchAssignment`: branch-level access assignment for a membership
- `AuthSession`: persisted login session with access/refresh token hashes and timeout tracking
- `UserInvitation`: invite token and onboarding state
- `PasswordResetToken`: reset-password flow state
- `AuditEvent`: append-only audit log

## Security Model

### Tenancy

- all authenticated requests resolve an agency context from the authenticated principal
- repository and service access are tenant-aware
- cross-agency access is denied
- branch-limited roles are restricted to assigned branches

### Sessions and tokens

- web auth uses secure, `HttpOnly`, `SameSite=Lax` cookies
- the backend also returns access token, refresh token, and session id in JSON responses
- access token TTL, refresh TTL, idle timeout, and absolute session duration are configurable
- refresh rotates the session forward
- logout revokes the active session

### Passwords and MFA

- password policy is enforced on invite acceptance, reset, and password change
- common passwords are blocked
- password reuse prevention is enforced
- MFA uses TOTP with one-time recovery codes
- agency MFA policy can require MFA for all users or selected roles

## Public HTTP API

This is the current frontend-facing API surface in this repository. It is mostly focused on authentication and security settings.

### Auth and session endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Email/password login. Returns tokens directly, or an MFA challenge if required. |
| `POST` | `/api/auth/login/mfa` | Completes MFA login with TOTP or recovery code. |
| `POST` | `/api/auth/refresh` | Rotates refresh token and returns a new session/token set. |
| `POST` | `/api/auth/logout` | Revokes current session, clears cookies, and redirects. |
| `GET` | `/api/auth/session` | Returns timeout metadata for warning and forced logout UX. |
| `GET` | `/api/auth/sessions` | Lists current and recent sessions for the signed-in user. |
| `DELETE` | `/api/auth/sessions/{sessionId}` | Revokes a non-current session. |

### Password and account security endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/auth/password-policy` | Returns password rules for UI validation and helper text. |
| `POST` | `/api/auth/forgot-password` | Starts reset flow without revealing whether the email exists. |
| `POST` | `/api/auth/reset-password` | Resets password from a valid reset token. |
| `POST` | `/api/auth/change-password` | Changes password for the current authenticated user. |

### MFA endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/auth/mfa/status` | Returns whether MFA is enabled and recovery-code count. |
| `POST` | `/api/auth/mfa/enrollment/start` | Starts TOTP enrollment after password re-auth. |
| `POST` | `/api/auth/mfa/enrollment/confirm` | Confirms MFA enrollment with a TOTP code. |

### Admin security settings endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/security/mfa-policy` | Reads the current agency MFA enforcement policy. |
| `PUT` | `/api/security/mfa-policy` | Updates the current agency MFA enforcement policy. |
| `GET` | `/api/security/admin-notifications` | Reads the current admin notification preferences. |
| `PUT` | `/api/security/admin-notifications` | Updates the current admin notification preferences. |

## Frontend Integration Guide

### 1. Login flow

Call `POST /api/auth/login` with:

```json
{
  "email": "user@example.com",
  "password": "YourPassword123!"
}
```

Possible outcomes:

- `200 OK` with `mfaRequired=false`: login is complete
- `200 OK` with `mfaRequired=true`: route user to MFA challenge screen
- `401 Unauthorized`: safe generic invalid-credentials response
- `429 Too Many Requests`: lockout or rate limit triggered

When MFA is not required, the response includes:

- `userId`
- `sessionId`
- `accessToken`
- `accessTokenExpiresAt`
- `refreshToken`
- `refreshTokenExpiresAt`

It also sets these cookies:

- `hhc_access_token`
- `hhc_refresh_token`
- `hhc_session_id`

### 2. MFA login flow

If login returns `mfaRequired=true`, call `POST /api/auth/login/mfa` with:

```json
{
  "challengeToken": "challenge-token-from-login",
  "totpCode": "123456"
}
```

Or use a recovery code:

```json
{
  "challengeToken": "challenge-token-from-login",
  "recoveryCode": "recovery-code"
}
```

### 3. Authenticated request pattern

For browser-based frontend work, the backend is designed for secure cookie auth.

For local frontend development, especially on plain `http://localhost`, secure cookies may not round-trip correctly. In that case use the token fields returned in JSON and send:

- `Authorization: Bearer <accessToken>`
- `X-Session-Id: <sessionId>`

For refresh:

- `X-Refresh-Token: <refreshToken>`
- `X-Session-Id: <sessionId>`

### 4. Session timeout UX

Use `GET /api/auth/session` to power:

- idle timeout banners
- "you will be signed out soon" warnings
- forced logout countdowns

The response includes:

- `idleTimeoutAt`
- `absoluteTimeoutAt`
- `forcedLogoutAt`
- `warningRequired`
- `secondsUntilForcedLogout`

### 5. Password-related screens

Use `GET /api/auth/password-policy` before rendering:

- reset password
- accept invite
- change password

That avoids hardcoding password rules in the frontend.

### 6. MFA settings screens

Suggested UI sequence:

1. Call `GET /api/auth/mfa/status`
2. If disabled, call `POST /api/auth/mfa/enrollment/start` with `currentPassword`
3. Show QR code or manual key using `otpauthUri` or `manualEntryKey`
4. Show recovery codes exactly once
5. Call `POST /api/auth/mfa/enrollment/confirm`

### 7. Admin security settings screens

For an agency admin UI:

- use `GET/PUT /api/security/mfa-policy` for agency-wide MFA enforcement
- use `GET/PUT /api/security/admin-notifications` for critical admin alert preferences

These APIs rely on the current authenticated agency membership and return `403` when the user lacks permission.

## Important Frontend Constraint

This backend contains more implemented business logic than currently exposed through public REST controllers.

Implemented in the backend service layer today:

- agency provisioning
- user invitations
- invite acceptance
- user directory queries
- user status changes
- role and branch assignment updates
- self-service profile updates
- branch creation and branch visibility rules

Currently exposed as public HTTP APIs:

- auth
- MFA
- session management
- password management
- admin security settings

That means a frontend can build a complete authentication and security experience right now, but not the full agency-admin application yet without adding additional controllers for user management, branch management, and invitation management.

## Suggested Frontend Pages

If you want to build an end-to-end frontend on top of the current backend first, start with this order:

1. Login
2. MFA challenge
3. Forgot password
4. Reset password
5. Change password
6. MFA settings
7. Active sessions
8. Session timeout warning
9. Admin MFA policy
10. Admin notification preferences

That sequence matches the existing HTTP surface and gives you a complete secure-auth shell before expanding into staff and branch administration.

## Error Handling Expectations

Common API behavior:

- `400` for invalid request state such as weak password, bad MFA code, or invalid token usage
- `401` for missing or invalid authentication
- `403` for permission denial or missing required MFA enrollment
- `404` for inaccessible managed resources where existence should not be leaked
- `410` for expired one-time flows such as reset or MFA challenge expiry
- `429` for login throttling and brute-force protection

The auth APIs intentionally use safe generic errors where account enumeration would be risky.

## Configuration

Key settings live in [application.properties](/Users/shiva/Documents/GitHub/homehealthcare/src/main/resources/application.properties).

Important groups:

- `security.session.*`
- `security.web.*`
- `security.login-protection.*`
- `security.mfa.*`
- `security.password-policy.*`
- `app.email.*`

The default `app.email.link-signing-secret=change-me-before-production` is for local development only and must be replaced in real environments.

## Documentation

Architecture decisions and security standards are indexed in [docs/README.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/README.md).

Most useful docs for frontend and full-stack work:

- [0004-secure-session-token-strategy.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/adr/0004-secure-session-token-strategy.md)
- [session-security-standard.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/session-security-standard.md)
- [web-security-hardening-standard.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/web-security-hardening-standard.md)
- [password-policy-standard.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/password-policy-standard.md)
- [role-permission-matrix.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/role-permission-matrix.md)
- [branch-scoping-guardrails.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/branch-scoping-guardrails.md)
- [tenant-isolation-guardrails.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/tenant-isolation-guardrails.md)

## Test Coverage Highlights

- auth and MFA regression suite: [build.gradle](/Users/shiva/Documents/GitHub/homehealthcare/build.gradle)
- CI workflow: [.github/workflows/ci.yml](/Users/shiva/Documents/GitHub/homehealthcare/.github/workflows/ci.yml)
- tenancy and branch isolation tests
- login, logout, password reset, and password change tests
- MFA enrollment and MFA challenge tests
- security hardening and audit coverage tests

## Next Backend APIs To Add

If the next goal is a full agency-admin frontend, the highest-value HTTP APIs to add next are:

1. user directory and filtering
2. invite user and resend/cancel invite
3. accept invite HTTP endpoint if you want the UI to use the backend directly for onboarding
4. user status management
5. role and branch assignment management
6. self-service profile endpoint
7. branch list and branch management endpoints

Those capabilities already exist largely in the service layer, so the remaining work is mainly API design, request/response mapping, and permission-aware controller exposure.
