# ADR 0004: Secure Session And Token Strategy

## Status

Accepted

## Story

`E1-S32 — Secure session/token strategy`

## Decision

The platform uses rotating opaque session tokens for web authentication.

The approved model is:

- short-lived opaque access token
- long-lived opaque refresh token
- persisted server-side session record
- secure `HttpOnly` cookies for browser delivery
- refresh-token rotation on session renewal

## Approved web delivery model

- `hhc_access_token` is delivered as a secure `HttpOnly` cookie.
- `hhc_refresh_token` is delivered as a secure `HttpOnly` cookie.
- `hhc_session_id` is delivered as a secure `HttpOnly` cookie and acts as a session binder/correlation identifier, not a standalone authenticator.
- Cookies use `SameSite=Lax` and `Path=/`.

## Lifetime model

- access token TTL: 15 minutes by default
- refresh token TTL: 30 days by default
- idle timeout: 30 minutes by default
- absolute max session duration: 12 hours by default
- both TTLs are configurable through `security.session.*`

## Refresh strategy

- browser clients call `POST /api/auth/refresh`
- refresh requires the active refresh token plus matching session id
- refresh revokes the old session with reason `TOKEN_REFRESHED`
- refresh issues a brand-new access token, refresh token, and session id

## Expiration enforcement

- expired access tokens are rejected for authenticated application actions
- expired refresh tokens are rejected for refresh
- idle-timed-out sessions are revoked and rejected
- absolute-duration-timed-out sessions are revoked and rejected
- revoked sessions are rejected regardless of token age

## Warning model

- browser clients may call `GET /api/auth/session` using the current access token
- the API returns `forcedLogoutAt`, `warningRequired`, and `secondsUntilForcedLogout`
- the warning window defaults to 2 minutes before forced logout

## Security consequences

- raw tokens are never stored directly in the database; hashed values are stored instead
- browser JavaScript cannot read authentication cookies
- refresh rotation reduces replay value of stolen refresh tokens after first successful reuse
