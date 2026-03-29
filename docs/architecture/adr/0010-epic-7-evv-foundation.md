# ADR 0010: Epic 7 EVV Foundation

## Status

Accepted

## Context

Epic 7 introduces compliance-sensitive visit verification and exception handling. The platform already has:

- auth, session, RBAC, branch scoping, and audit foundations from Epic 1
- scheduling and visit ownership context from Epic 5
- caregiver mobile execution, signatures, field artifacts, and offline support from Epic 6

Epic 7 must now add explicit EVV and missed-visit foundations without prematurely absorbing:

- structured note-template workflows from Epic 8
- generalized messaging expansion from Epic 9
- the fuller QA review workspace from Epic 10
- downstream billing and claims workflows from later epics

## Decision

Epic 7 EVV foundation will be modeled around explicit, auditable proof-of-visit and exception contracts:

- EVV verification session
- clock event
- device metadata snapshot
- geofence tolerance rule
- geofence evaluation
- signature verification link
- missed-visit record
- visit exception record
- supervisor notification event
- escalation request
- EVV compliance projection

The shared foundation defines:

- EVV entity categories
- EVV verification lifecycle states
- geofence evaluation outcomes
- EVV compliance outcomes
- EVV operation guardrails
- Epic 7 EVV audit action types
- Epic 7 EVV target types
- a geofence evaluation result contract
- an EVV compliance projection contract

Epic 7 permissions are explicit and split between caregiver submission and operational review:

- `VIEW_OWN_EVV`
- `SUBMIT_OWN_EVV`
- `MANAGE_EVV_EXCEPTIONS`
- `VIEW_MISSED_VISITS`
- `RESOLVE_MISSED_VISITS`
- `RECEIVE_EVV_NOTIFICATIONS`

## Consequences

### Positive

- Epic 7 can build on a consistent EVV vocabulary before full APIs and persistence land.
- Missed-visit and exception workflows have explicit audit and authorization hooks from day one.
- Later QA and revenue-readiness workflows can consume a stable EVV compliance projection.

### Negative

- Phase A does not yet implement real EVV APIs or persistence models.
- Geofence policy storage and supervisor queue behavior remain later-phase work.

## Scope boundary

Epic 7 foundation does not implement:

- full note-template or form workflows
- generalized communication workspace expansion
- QA review queues
- revenue-readiness or claims behavior
