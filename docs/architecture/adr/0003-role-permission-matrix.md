# ADR 0003: Role Permission Matrix

## Status

Accepted

## Story

`E1-S27 — Define role-permission matrix`

## Context

Epic 1 has already established the agency, branch, membership, and MFA foundations. The next authorization work needs a formal permission model so feature teams implement access rules consistently instead of encoding one-off role checks per module.

This ADR is the approval record for the first HomeHealthCare role-permission matrix. It must be treated as the source of truth until a later ADR revises it.

## Decision

The platform will use:

- agency roles assigned through `AgencyMembership`
- module-area permissions expressed as CRUD-style capabilities
- explicit scope classification for each capability:
  - `agency`
  - `branch`
  - `self`
  - `platform`

The initial implementation target is a role-to-permission matrix documented in [role-permission-matrix.md](/Users/shiva/Documents/GitHub/homehealthcare/docs/architecture/standards/role-permission-matrix.md).

## Approved roles

The approved tenant roles are:

- `AGENCY_OWNER`
- `BRANCH_ADMIN`
- `SCHEDULER_COORDINATOR`
- `CAREGIVER`
- `QA_CLINICAL_REVIEWER`
- `BILLING_BACK_OFFICE`
- `READ_ONLY_AUDITOR`

These names match the current `AgencyRole` enum and are approved for RBAC implementation.

## Scope model

### Agency scope

Permissions at agency scope may access any allowed record in the current tenant, subject to tenant isolation.

### Branch scope

Permissions at branch scope are limited to the actor's assigned branches unless a separate agency-wide rule exists for that role.

### Self scope

Permissions at self scope apply only to the acting user's own identity or account artifacts.

### Platform scope

Permissions at platform scope are reserved for internal operator workflows and are not granted by agency membership roles.

## Authorization rules

- Tenant isolation always applies first.
- Branch restrictions apply on top of tenant isolation for branch-scoped capabilities.
- A role may have the same module capability at different scope than another role.
- If a capability is not listed for a role, access is denied by default.

## Approval outcome

The initial matrix in the companion standard is approved for implementation use in Epic 1 authorization work.

Any change to:

- role names
- scope semantics
- module capability groups
- cross-role privilege boundaries

requires an ADR update before implementation diverges.

## Consequences

- Feature work can implement policy checks against a stable role-permission contract.
- Test plans can be derived directly from the approved matrix.
- Future fine-grained permissions can extend this model without changing the tenant and branch foundations already in place.
