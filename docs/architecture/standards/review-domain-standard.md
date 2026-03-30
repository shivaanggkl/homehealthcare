# Review Domain Standard

## Status

Accepted

## Epic

Epic 10: QA / review workspace

## Purpose

This standard defines the shared Epic 10 backend vocabulary for review queues, reviewer assignment, completeness findings, and signoff flows.

## Core categories

Use [ReviewEntityCategory](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/review/foundation/ReviewEntityCategory.java) for Epic 10 review-owned entities.

The approved categories are:

- `REVIEW_WORK_ITEM`
- `REVIEW_ASSIGNMENT`
- `REVIEW_FINDING`
- `COMPLETENESS_CHECK_RESULT`
- `MISSING_FIELD_RESULT`
- `REVIEW_DECISION`
- `RETURN_FOR_FIX_EVENT`
- `SIGNOFF_REQUEST`
- `REVIEW_STATUS_PROJECTION`

## Review decision vocabulary

Use [ReviewDecisionType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/review/foundation/ReviewDecisionType.java) for explicit decision actions.

Allowed values:

- `APPROVE`
- `REJECT`
- `RETURN_FOR_FIX`
- `REQUEST_SIGNOFF`

Review decisions must be explicit and must not be inferred only from free-text reviewer notes.

## Queue and finding vocabulary

Review queues may include work triggered from documentation, EVV, missed-visit, or visit-execution context, but Epic 10 review entities own the review lifecycle itself.

Completeness and missing-field outputs should expose structured codes and reviewer-visible explanations, not only narrative text.

## Privacy and audit rules

- source-document content must not be copied wholesale into Epic 10 audit metadata
- reviewer notes may be persisted in review-owned records but only identifiers and compact metadata should enter audit events
- review summaries returned to clients must expose only the minimum patient, visit, and document context needed for authorized triage
- returned-for-fix workflows must preserve prior finding context rather than overwriting it

## Audit taxonomy

Use [Epic10ReviewAuditAction](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/review/foundation/Epic10ReviewAuditAction.java) and [Epic10ReviewTargetType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/review/foundation/Epic10ReviewTargetType.java).

Required Epic 10 audit actions:

- review item created
- review assigned
- review reassigned
- review decision recorded
- review returned for fix
- signoff requested
- completeness recalculated
