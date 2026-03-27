# Web Security Hardening Standard

## Status

Accepted

## Story

`E1-S35 — Security headers and baseline hardening`

## Purpose

This standard defines the minimum browser-facing web protections for the HomeHealthCare platform.

## Required headers

- `Content-Security-Policy` must restrict content to the approved origin policy.
- `X-Frame-Options` must be `DENY`.
- `X-Content-Type-Options` must be `nosniff`.
- `Referrer-Policy` must be `no-referrer`.

## CSRF strategy

- Because browser authentication uses secure cookies, unsafe cookie-authenticated requests must require CSRF protection.
- The approved pattern is double-submit CSRF using Spring Security's cookie token repository.
- Bearer-token requests sent through the `Authorization` header are treated as non-cookie API requests and are exempt from CSRF enforcement.

## CORS strategy

- CORS must be configured through environment properties.
- Allowed origins must be explicit and environment-specific.
- Credentials are allowed only for configured origins.
- Wildcard origins are not permitted when credentials are enabled.

## Cookie rules

- Sensitive auth cookies must be `Secure`.
- Sensitive auth cookies must be `HttpOnly`.
- Sensitive auth cookies must use `SameSite=Lax` unless a later ADR explicitly approves another mode.

## Secret-exposure rule

- Frontend-facing APIs must not expose password hashes, token hashes, MFA secrets, or recovery-code material.
- Web authentication should rely on secure cookies rather than exposing browser credentials to client-side JavaScript where possible.
