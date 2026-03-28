# HomeHealthCare Documentation

## Architecture

- `docs/architecture/adr/0001-tenancy-model.md` defines the approved multi-tenant model for `E1-S01`.
- `docs/architecture/adr/0002-internal-super-admin-model.md` defines the approved internal operator model for `E1-S05`.
- `docs/architecture/adr/0003-role-permission-matrix.md` records the approved RBAC model for `E1-S27`.
- `docs/architecture/adr/0004-secure-session-token-strategy.md` records the approved auth session model for `E1-S32`.
- `docs/architecture/erd/e1-s01-tenancy-model.mmd` is the source ERD for the tenancy foundation.
- `docs/architecture/standards/entity-scoping-standard.md` defines `agency_id` and `branch_id` standards for future entities.
- `docs/architecture/standards/migration-pattern.md` defines the migration checklist for tenant-aware tables.
- `docs/architecture/standards/branch-scoping-guardrails.md` defines branch visibility rules for `E1-S08`.
- `docs/architecture/standards/role-permission-matrix.md` defines CRUD permissions and scope rules for tenant roles.
- `docs/architecture/standards/session-security-standard.md` defines cookie, refresh, and expiration rules for authenticated web sessions.
- `docs/architecture/standards/password-policy-standard.md` defines setup, reset, and change-password rules, including common-password blocking and reuse prevention.
- `docs/architecture/standards/audit-event-standard.md` defines the unified audit-event schema, success/failure outcomes, and retention rules.
- `docs/architecture/standards/system-email-standard.md` defines invite, password reset, and security alert email template rules and signed-link requirements.
- `docs/architecture/standards/admin-security-notification-standard.md` defines admin alert types, delivery preferences, and notification audit rules.
- `docs/architecture/standards/security-settings-contract.md` defines the consolidated Epic 1 security settings contract and the session-policy scope boundary.
- `docs/architecture/standards/web-security-hardening-standard.md` defines headers, CSRF, CORS, and secret-exposure rules for browser clients.
- `docs/architecture/standards/mfa-admin-guardrails.md` defines admin MFA visibility and enforcement rules for `E1-S26`.
- `docs/architecture/standards/tenant-isolation-guardrails.md` defines repository and service isolation rules for `E1-S07`.
- `docs/reviews/epic-1-security-readiness-review.md` records the pre-production security checklist, resolved high-risk findings, and Epic 1 sign-off for `E1-S48`.

These files are the source of truth for Epic 1 design decisions until implementation starts.
