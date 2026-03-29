# ADR 0008: Epic 5 Scheduling Domain Foundation

## Status

Accepted

## Context

Epic 5 introduces the scheduling engine. The platform already has:

- tenant, branch, and RBAC foundations from Epic 1
- configuration data from Epic 2
- patient identity from Epic 3
- caregiver workforce inputs from Epic 4

Scheduling must now become a first-class backend domain without prematurely absorbing:

- mobile execution
- EVV and GPS proof
- signatures
- visit-note completion
- revenue-readiness logic

Those later behaviors should reference Epic 5 schedule records rather than replace or bypass them.

## Decision

Epic 5 scheduling foundation will be modeled around explicit, auditable operational records:

- visit occurrence
- recurring visit rule
- caregiver assignment
- open shift
- reschedule event
- cancellation event
- conflict evaluation
- travel-awareness evaluation

The shared foundation defines:

- schedule entity categories
- visit lifecycle states
- conflict outcome levels
- operation guardrails
- scheduling audit action types
- scheduling target types
- a travel-awareness contract suitable for public scheduling APIs

Scheduling permissions are explicit and separate from generic workforce permissions:

- `VIEW_SCHEDULING_WORKSPACE`
- `MANAGE_SCHEDULE_VISITS`
- `ASSIGN_CAREGIVERS`
- `MANAGE_OPEN_SHIFTS`
- `RESCHEDULE_VISITS`
- `CANCEL_VISITS`
- `VIEW_SCHEDULE_CONFLICTS`

## Consequences

### Positive

- Epic 5 can build on a consistent scheduling vocabulary before board APIs or mutations exist.
- Scheduling mutations become audit-visible from the start.
- Travel-awareness and conflict contracts are public-API-safe before algorithm depth expands.
- Later Epics 6 and 7 can attach field execution and EVV behavior to schedule records instead of inventing parallel workflow state.

### Negative

- Epic 5 Phase A does not yet implement real scheduling persistence or board APIs.
- Travel awareness is only a contract in this phase, not a full routing engine.

## Scope boundary

Epic 5 foundation does not implement:

- drag-and-drop UI behavior
- mobile visit execution
- EVV GPS proof
- patient/caregiver signatures
- visit notes
- revenue or claim workflows
