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
- `docs/architecture/adr/0010-epic-7-evv-foundation.md` records the approved shared foundation for Epic 7 EVV and exception modules.
- `docs/architecture/adr/0011-epic-8-documentation-domain-foundation.md` records the approved shared foundation for Epic 8 notes, forms, and task-completion modules.
- `docs/architecture/adr/0012-epic-9-messaging-foundation.md` records the approved shared foundation for Epic 9 messaging and coordination modules.
- `docs/architecture/adr/0013-epic-10-review-foundation.md` records the approved shared foundation for Epic 10 QA and review modules.
- `docs/architecture/adr/0014-epic-11-compliance-foundation.md` records the approved shared foundation for Epic 11 clinical compliance modules.
- `docs/architecture/adr/0015-epic-12-patient-event-foundation.md` records the approved shared foundation for Epic 12 incident, infection, and wound modules.
- `docs/architecture/adr/0016-epic-13-care-progression-foundation.md` records the approved shared foundation for Epic 13 goals, interventions, and care-progression modules.
- `docs/architecture/adr/0017-epic-14-revenue-readiness-foundation.md` records the approved shared foundation for Epic 14 revenue-readiness and financial-ops modules.
- `docs/architecture/adr/0018-epic-15-analytics-foundation.md` records the approved shared foundation for Epic 15 dashboard and analytics modules.
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
- `docs/architecture/standards/evv-domain-standard.md` defines shared categories, EVV contracts, and audit expectations for Epic 7 visit verification modules.
- `docs/architecture/standards/evv-lifecycle-standard.md` defines the shared verification lifecycle, geofence outcomes, and guardrail rules for Epic 7 EVV entities.
- `docs/architecture/standards/documentation-domain-standard.md` defines shared categories, field types, and audit expectations for Epic 8 documentation modules.
- `docs/architecture/standards/documentation-lifecycle-standard.md` defines the shared documentation lifecycle, submission, and printable-summary guardrail rules for Epic 8 documentation entities.
- `docs/architecture/standards/messaging-domain-standard.md` defines shared thread, delivery, escalation, and audit expectations for Epic 9 messaging modules.
- `docs/architecture/standards/messaging-lifecycle-standard.md` defines the shared lifecycle, participant, and guardrail rules for Epic 9 messaging entities.
- `docs/architecture/standards/review-domain-standard.md` defines shared review categories, decision vocabulary, and audit expectations for Epic 10 review modules.
- `docs/architecture/standards/review-lifecycle-standard.md` defines the shared review lifecycle, assignment, and guardrail rules for Epic 10 review entities.
- `docs/architecture/standards/compliance-domain-standard.md` defines shared checklist, acknowledgment, certification, reminder, and audit expectations for Epic 11 compliance modules.
- `docs/architecture/standards/compliance-lifecycle-standard.md` defines the shared compliance lifecycle, readiness vocabulary, and guardrail rules for Epic 11 compliance entities.
- `docs/architecture/standards/patient-event-domain-standard.md` defines shared incident, infection, wound, evidence, follow-up, alert, and audit expectations for Epic 12 patient-event modules.
- `docs/architecture/standards/patient-event-lifecycle-standard.md` defines the shared lifecycle, longitudinal history, alert, and guardrail rules for Epic 12 patient-event entities.
- `docs/architecture/standards/care-progression-domain-standard.md` defines shared template, goal, intervention, sync, and audit expectations for Epic 13 care-progression modules.
- `docs/architecture/standards/care-progression-lifecycle-standard.md` defines the shared lifecycle, version-history, sync, and guardrail rules for Epic 13 care-progression entities.
- `docs/architecture/standards/revenue-readiness-domain-standard.md` defines shared readiness, export, authorization-usage, and audit expectations for Epic 14 revenue-readiness modules.
- `docs/architecture/standards/revenue-readiness-lifecycle-standard.md` defines the shared readiness/export lifecycle and guardrail rules for Epic 14 revenue-readiness entities.
- `docs/architecture/standards/analytics-domain-standard.md` defines shared metric categories, source-of-truth linkage, refresh modes, and audit expectations for Epic 15 analytics modules.
- `docs/architecture/standards/analytics-lifecycle-standard.md` defines the shared snapshot, trend, and refresh lifecycle guardrails for Epic 15 analytics entities.
- `docs/architecture/standards/web-security-hardening-standard.md` defines headers, CSRF, CORS, and secret-exposure rules for browser clients.
- `docs/architecture/standards/mfa-admin-guardrails.md` defines admin MFA visibility and enforcement rules for `E1-S26`.
- `docs/architecture/standards/tenant-isolation-guardrails.md` defines repository and service isolation rules for `E1-S07`.
- `docs/reviews/epic-1-security-readiness-review.md` records the pre-production security checklist, resolved high-risk findings, and Epic 1 sign-off for `E1-S48`.

## Testing

- `docs/testing/epic-1-2-manual-e2e-guide.md` is the step-by-step manual validation guide for Epic 1 and Epic 2 across frontend and backend behavior.

These files are the source of truth for Epic 1 design decisions until implementation starts.
