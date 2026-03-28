# Security Settings Contract

## Status

Accepted

## Story

`BE-11 — Consolidated security settings contract`

## Purpose

This standard defines the owner-facing Epic 1 security settings contract exposed by the backend.

## Public API

- `GET /api/security/settings`
- `GET /api/security/mfa-policy`
- `PUT /api/security/mfa-policy`
- `GET /api/auth/password-policy`

## Consolidated contract rules

- `GET /api/security/settings` is the read model for the Epic 1 security settings screen.
- The response includes:
  - current agency MFA policy
  - current password policy
  - current session timeout policy
- Session timeout values come from runtime configuration, not tenant-persisted settings.

## Scope boundary

- Epic 1 does not expose a public API to edit:
  - idle timeout
  - absolute session duration
  - warning window
- Those values remain deployment-time runtime configuration under `security.session.*`.
- The consolidated response must therefore expose session policy as read-only and make that boundary explicit.

## Authorization

- Only roles already permitted to manage agency MFA policy may access the consolidated security settings contract.
- The consolidated contract must never expose:
  - TOTP secrets
  - recovery codes
  - password hashes
  - session token material

## Test rules

- Tests must prove authorized access for an admin role.
- Tests must prove denied access for a non-admin role.
- Tests must prove the session policy section is explicit about its read-only scope boundary.
