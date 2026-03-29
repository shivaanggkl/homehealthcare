# ADR 0007: Epic 4 Workforce Domain Foundation

## Status

Accepted

## Context

Epic 4 introduces caregiver workforce management as a first-class domain.

The requirements place caregiver profile, credentials, languages, skills, geography preferences,
shift preferences, availability, PTO or unavailability, and performance indicators into one
coherent workforce module. Epic 4 must stop short of scheduling-board logic, drag-and-drop
assignment, and route optimization because those belong to Epic 5.

The platform already has:

- Epic 1 tenancy, RBAC, audit, and session foundations
- Epic 2 reusable workforce configuration catalogs such as skills and certifications
- Epic 3 audit-sensitive domain patterns for long-lived business entities

Epic 4 needs a shared backend foundation so later workforce entities do not invent competing
lifecycle rules, audit conventions, or schedulability conflict behavior.

## Decision

Epic 4 will use a dedicated `workforce.foundation` package that defines:

- workforce entity categories
- workforce lifecycle states
- workforce operation guardrails
- workforce performance indicator types
- standardized Epic 4 audit action types
- standardized Epic 4 audit target types
- a shared `WorkforceAuditService`

The authorization layer is extended with workforce-specific permissions for:

- viewing the workforce directory
- managing caregiver profiles
- managing caregiver credentials
- managing caregiver availability
- managing caregiver unavailability
- viewing caregiver performance

These permissions are agency-wide for `AGENCY_OWNER` and branch-scoped for
`BRANCH_ADMIN` and `SCHEDULER_COORDINATOR`.

## Consequences

Positive:

- Epic 4 modules share one consistent lifecycle and audit model
- later workforce APIs can build on existing permission and audit foundations
- branch-scoped workforce access is explicit before scheduling logic arrives
- performance indicators are treated as derived summaries, not ad hoc mutable fields

Tradeoffs:

- Epic 4 still requires later implementation for actual caregiver entities and APIs
- performance indicators are intentionally defined as a contract first, not a reporting engine
- overlap guardrail behavior is documented before all availability entities exist

## Follow-on work

Next Epic 4 phases should implement:

- caregiver profile entities
- credentials, skills, language, geography, availability, and PTO models
- public workforce APIs
- regression coverage for representative CRUD and conflict behavior
