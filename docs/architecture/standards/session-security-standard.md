# Session Security Standard

## Status

Accepted

## Story

`E1-S32 — Secure session/token strategy`

## Purpose

This standard defines how authenticated web sessions must be issued, refreshed, expired, and revoked.

## Required controls

- Web authentication cookies must be `Secure` and `HttpOnly`.
- Browser auth cookies must use `SameSite=Lax` unless a later ADR approves a different cross-site model.
- Access tokens must be short-lived.
- Refresh tokens must be long-lived but rotatable.
- Server-side session persistence must support revocation and expiration timestamps.
- Idle timeout and absolute session duration must both be enforced server-side.

## Token model

- Access token authorizes normal authenticated API access.
- Refresh token authorizes only session renewal.
- Session id binds the refresh attempt to the persisted session and must not be treated as a standalone bearer credential.

## API rules

- `POST /api/auth/login` issues a new token pair and session cookies.
- `POST /api/auth/refresh` rotates the refresh token and session.
- `POST /api/auth/logout` revokes the current session and clears cookies.
- `GET /api/auth/session` exposes warning metadata for controlled UI logout prompts.
- Protected account-management flows must rely on an unexpired access token.

## Expiration rules

- Access-token expiration must be enforced during authenticated request resolution.
- Refresh-token expiration must be enforced during refresh.
- Idle timeout must be enforced from last authenticated activity.
- Absolute timeout must be enforced from original session issue time.
- Cookie max-age must not exceed the corresponding server-side token expiration.

## Audit rules

- login, refresh, logout, password changes, and session-wide revocations must be auditable.
- idle and absolute timeout revocations must create timeout audit events.

## Configuration

- `security.session.access-token-ttl`
- `security.session.refresh-token-ttl`
- `security.session.idle-timeout`
- `security.session.absolute-session-duration`
- `security.session.warning-window`

## Test requirements

- tests must prove secure cookie attributes are present
- tests must prove expired access tokens are rejected
- tests must prove refresh rotates the old session out of use
