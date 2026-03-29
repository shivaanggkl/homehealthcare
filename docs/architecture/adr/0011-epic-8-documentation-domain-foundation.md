# ADR 0011: Epic 8 Documentation Foundation

## Status

Accepted

## Context

Epic 8 introduces structured visit notes, form templates, task completion, and printable summaries. The platform already has:

- auth, session, RBAC, branch scoping, and audit foundations from Epic 1
- visit, patient, workforce, mobile execution, and EVV context from Epics 3 through 7

Epic 8 must add explicit documentation foundations without prematurely absorbing:

- generalized messaging expansion from Epic 9
- QA review queue and reviewer assignment workflows from Epic 10
- deeper care-plan and goals/interventions workflows from Epic 13
- revenue-cycle workflows from later epics

## Decision

Epic 8 documentation foundation will be modeled around explicit, auditable structured-documentation contracts:

- visit note template
- form template
- reusable task library item
- template task instance
- visit documentation record
- note field response
- task completion response
- printable summary projection

The shared foundation defines:

- documentation entity categories
- documentation record lifecycle states
- documentation field types
- documentation operation guardrails
- Epic 8 documentation audit action types
- Epic 8 documentation target types

Epic 8 permissions are explicit and split between configuration, field capture, review, and print-safe access:

- `VIEW_DOCUMENTATION_WORKSPACE`
- `MANAGE_DOCUMENTATION_TEMPLATES`
- `MANAGE_DOCUMENTATION_TASK_LIBRARY`
- `VIEW_VISIT_DOCUMENTATION`
- `DRAFT_VISIT_DOCUMENTATION`
- `SUBMIT_VISIT_DOCUMENTATION`
- `AMEND_VISIT_DOCUMENTATION`
- `GENERATE_PRINTABLE_DOCUMENTATION_SUMMARY`

## Consequences

### Positive

- Epic 8 can build templates, draft saves, required-field validation, and printable summaries on a shared vocabulary.
- Audit and authorization hooks exist before full template and documentation APIs land.
- Later QA and billing-adjacent workflows can consume stable documentation lifecycle states.

### Negative

- Phase A does not yet implement real template persistence or documentation APIs.
- Print rendering remains a later-phase projection concern rather than a full document engine.

## Scope boundary

Epic 8 foundation does not implement:

- generalized messaging expansion
- QA queue ownership and review assignment
- goals/interventions or broader care-plan workflows
- claims, billing, or revenue-readiness behaviors
