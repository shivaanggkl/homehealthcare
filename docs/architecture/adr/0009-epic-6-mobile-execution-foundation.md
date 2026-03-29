# ADR 0009: Epic 6 Mobile Execution Foundation

## Status

Accepted

## Context

Epic 6 introduces the caregiver mobile app. The platform already has:

- auth, session, RBAC, branch scoping, and audit foundations from Epic 1
- patient identity and attachment patterns from Epic 3
- caregiver identity and availability inputs from Epic 4
- visit, assignment, and schedule context from Epic 5

Mobile execution must now become a first-class backend domain without prematurely absorbing:

- full EVV compliance workflows
- geofence tolerance policy engines
- missed-visit escalation trees
- revenue-readiness review logic

Those later behaviors belong primarily to Epic 7 and downstream modules.

## Decision

Epic 6 mobile foundation will be modeled around explicit, auditable field-execution contracts:

- mobile device session
- today-work query
- visit execution session
- route stop projection
- patient summary snapshot
- care-instruction snapshot
- task checklist entry
- quick note entry
- field photo artifact
- field signature artifact
- incident report
- message thread summary
- offline sync envelope

The shared foundation defines:

- mobile execution entity categories
- visit execution lifecycle states
- sync result dispositions
- operation guardrails
- Epic 6 mobile audit action types
- Epic 6 mobile target types
- a mobile session contract
- an offline sync envelope and result contract

Epic 6 permissions are explicit and caregiver-centered:

- `VIEW_OWN_MOBILE_VISITS`
- `EXECUTE_OWN_VISITS`
- `SUBMIT_MOBILE_VISIT_DOCUMENTATION`
- `UPLOAD_MOBILE_VISIT_ARTIFACTS`
- `CREATE_MOBILE_INCIDENTS`
- `VIEW_MOBILE_MESSAGES`
- `SEND_MOBILE_MESSAGES`

## Consequences

### Positive

- Epic 6 can build on a consistent mobile vocabulary before real caregiver APIs are implemented.
- Offline sync and idempotency expectations are defined before field mutations exist.
- Audit visibility starts at the mobile foundation rather than being retrofitted later.
- Epic 7 can extend visit execution into deeper EVV and exception workflows without redefining the base visit-session model.

### Negative

- Phase A does not yet implement real mobile APIs or persistence.
- Offline support is only a contract in this phase, not a full queueing implementation.

## Scope boundary

Epic 6 foundation does not implement:

- full EVV missed-visit workflows
- geofence tolerance evaluation
- physician signature dependencies
- QA review queues
- billing readiness
