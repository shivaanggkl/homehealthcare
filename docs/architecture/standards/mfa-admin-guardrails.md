# MFA Admin Guardrails

## Status

Accepted

## Story

`E1-S26 — Admin visibility into MFA status`

## Purpose

This standard defines what agency admins may see and manage for MFA without exposing user secret material.

## Visibility rules

- Agency user directories may expose MFA state only as operational metadata such as enabled status, enrolled timestamp, or remaining recovery-code count.
- Admin-facing APIs must never expose `mfa_secret`, TOTP manual entry keys, raw recovery codes, or OTP seeds.
- Recovery codes are shown only once to the enrolled end user during self-service enrollment.

## Policy management rules

- Agency MFA enforcement is configured at the agency level.
- Supported modes are `OFF`, `ALL_USERS`, and `SELECTED_ROLES`.
- `SELECTED_ROLES` must include at least one agency role.
- `OFF` and `ALL_USERS` must not carry a role list.

## Authorization rules

- Only active agency admins may view or change agency MFA enforcement policy.
- MFA policy changes are scoped to the current tenant agency.
- Cross-agency policy access must fail through normal tenant context enforcement.

## Audit rules

- Every admin-initiated MFA policy change must emit an audit event with actor membership, agency, mode, and selected roles.
- Viewing MFA-enabled status in the directory does not reveal secrets and therefore does not require separate secret-access auditing.
