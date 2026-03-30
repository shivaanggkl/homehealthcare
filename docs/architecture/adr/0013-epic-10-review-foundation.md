# ADR 0013: Epic 10 Review Foundation

## Status

Accepted

## Context

Epic 10 introduces a dedicated QA and review workspace for queue-driven review, reviewer assignment, completeness evaluation, return-for-fix loops, and signoff requests.

Earlier epics already provide documentation, EVV, messaging, patient context, and audit infrastructure. Epic 10 needs a shared review foundation before queue entities and review APIs can be built safely.

## Decision

Epic 10 uses a single review foundation with:

- explicit review-owned entity categories
- explicit lifecycle vocabulary for pending, assigned, in-review, returned, resubmitted, approved, rejected, and signoff-requested work
- explicit review-decision vocabulary for approve, reject, return-for-fix, and request-signoff flows
- shared audit taxonomy for queue creation, assignment, decision, return-for-fix, signoff, and completeness recalculation
- explicit permissions for review workspace visibility, exception-queue visibility, review assignment, review decisions, signoff requests, and audit-context visibility

Audit metadata for Epic 10 must stay content-safe.

Reviewer notes and source-document content are not treated as export-safe audit metadata.

## Consequences

- QA and reviewer flows can build on one lifecycle instead of inventing separate status logic per feature
- later compliance and revenue modules can consume stable review signals without redefining assignment or decision actions
- branch-aware authorization remains explicit for review queues and exception visibility
- the audit API can expose Epic 10 review events without any new audit transport

## Follow-up

Phase B will add the concrete review work-item, assignment, finding, and signoff persistence model.

Phase C will add the public APIs for queue listing, assignment, decisions, findings, and review history.
