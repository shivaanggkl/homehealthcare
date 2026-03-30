# Review Lifecycle Standard

## Status

Accepted

## Epic

Epic 10: QA / review workspace

## Purpose

This standard defines the shared Epic 10 lifecycle, assignment, and guardrail rules for review entities.

## Lifecycle vocabulary

Use [ReviewLifecycleStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/review/foundation/ReviewLifecycleStatus.java) for review-work lifecycle state.

Allowed values:

- `PENDING_REVIEW`
- `ASSIGNED`
- `IN_REVIEW`
- `RETURNED_FOR_FIX`
- `RESUBMITTED`
- `APPROVED`
- `REJECTED`
- `SIGNOFF_REQUESTED`
- `SIGNOFF_COMPLETED`

## Lifecycle rules

- new reviewable work enters `PENDING_REVIEW` unless immediately assigned by a valid backend workflow
- assignment transitions work into `ASSIGNED`
- active reviewer work may transition into `IN_REVIEW`
- return-for-fix must move work into `RETURNED_FOR_FIX` and preserve prior finding history
- corrected work re-entering the queue moves into `RESUBMITTED`
- only explicit review decisions may move work into `APPROVED` or `REJECTED`
- signoff requests move work into `SIGNOFF_REQUESTED`
- signoff completion may move work into `SIGNOFF_COMPLETED` or another explicitly modeled terminal state

## Guardrails

Use [ReviewOperationGuardrail](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/review/foundation/ReviewOperationGuardrail.java).

Required Epic 10 guardrails:

- cross-tenant review access blocked
- cross-branch review assignment blocked
- review decisions require authorized assignment context
- return-for-fix must preserve finding history
- signoff requests must not skip review audit
- review summaries must exclude excess patient detail

## Assignment rules

- assignment must be explicit and auditable
- unauthorized assignment, reassignment, or decision actions must fail with controlled errors
- inactive or out-of-scope memberships must not be treated as valid review actors

## Findings and recalculation rules

- completeness and missing-field findings must be durable enough to support later review history
- recalculation must refresh the current review picture without silently deleting historical review decisions
