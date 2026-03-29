# HomeHealthCare Documentation

## Architecture

- `docs/architecture/adr/0001-tenancy-model.md` defines the approved multi-tenant model for `E1-S01`.
- `docs/architecture/adr/0002-internal-super-admin-model.md` defines the approved internal operator model for `E1-S05`.
- `docs/architecture/adr/0003-role-permission-matrix.md` records the approved RBAC model for `E1-S27`.
- `docs/architecture/adr/0004-secure-session-token-strategy.md` records the approved auth session model for `E1-S32`.
- `docs/architecture/adr/0005-epic-2-configuration-foundation.md` records the approved shared foundation for Epic 2 configuration modules.
- `docs/architecture/adr/0006-epic-3-patient-domain-foundation.md` records the approved shared foundation for Epic 3 patient modules.
- `docs/architecture/adr/0007-epic-4-workforce-domain-foundation.md` records the approved shared foundation for Epic 4 workforce modules.
- `docs/architecture/adr/0008-epic-5-scheduling-domain-foundation.md` records the approved shared foundation for Epic 5 scheduling modules.
- `docs/architecture/adr/0009-epic-6-mobile-execution-foundation.md` records the approved shared foundation for Epic 6 caregiver mobile modules.
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
- `docs/architecture/standards/configuration-domain-standard.md` defines shared categories, fields, and audit expectations for Epic 2 configuration modules.
- `docs/architecture/standards/configuration-lifecycle-standard.md` defines the shared lifecycle, effective-date, and dependency rules for Epic 2 configuration entities.
- `docs/architecture/standards/patient-domain-standard.md` defines shared categories, fields, and audit expectations for Epic 3 patient modules.
- `docs/architecture/standards/patient-lifecycle-standard.md` defines the shared lifecycle, duplicate, archival, and attachment rules for Epic 3 patient entities.
- `docs/architecture/standards/workforce-domain-standard.md` defines shared categories, fields, and audit expectations for Epic 4 workforce modules.
- `docs/architecture/standards/workforce-lifecycle-standard.md` defines the shared lifecycle, schedulability, and guardrail rules for Epic 4 workforce entities.
- `docs/architecture/standards/scheduling-domain-standard.md` defines shared categories, travel-awareness contract rules, and audit expectations for Epic 5 scheduling modules.
- `docs/architecture/standards/scheduling-lifecycle-standard.md` defines the shared lifecycle, conflict-outcome, and guardrail rules for Epic 5 scheduling entities.
- `docs/architecture/standards/mobile-execution-domain-standard.md` defines shared categories, mobile session, sync-contract, and audit expectations for Epic 6 caregiver mobile modules.
- `docs/architecture/standards/mobile-execution-lifecycle-standard.md` defines the shared execution lifecycle, sync-outcome, and guardrail rules for Epic 6 caregiver mobile entities.
- `docs/architecture/standards/web-security-hardening-standard.md` defines headers, CSRF, CORS, and secret-exposure rules for browser clients.
- `docs/architecture/standards/mfa-admin-guardrails.md` defines admin MFA visibility and enforcement rules for `E1-S26`.
- `docs/architecture/standards/tenant-isolation-guardrails.md` defines repository and service isolation rules for `E1-S07`.
- `docs/reviews/epic-1-security-readiness-review.md` records the pre-production security checklist, resolved high-risk findings, and Epic 1 sign-off for `E1-S48`.

## Testing

- `docs/testing/epic-1-2-manual-e2e-guide.md` is the step-by-step manual validation guide for Epic 1 and Epic 2 across frontend and backend behavior.

These files are the source of truth for Epic 1 design decisions until implementation starts.
