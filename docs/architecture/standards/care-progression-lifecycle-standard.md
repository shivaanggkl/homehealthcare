# Care-Progression Lifecycle Standard

## Purpose

This standard defines Epic 13 lifecycle and guardrail expectations for goal and care-progression records.

## Guardrails

- cross-tenant goal access is blocked
- cross-branch goal access is blocked unless explicitly authorized
- goal state transitions must preserve append-oriented version history
- progress notes must append instead of overwrite prior clinical narrative
- care-plan synchronization must not mutate or corrupt historical goal versions
- summaries and command-center projections must exclude excess patient detail

## Goal State Expectations

- `ACTIVE` goals may transition to `COMPLETED`, `UNMET`, `NOT_ATTAINED`, or `CANCELLED`
- terminal goal states must preserve the resolved history and actor context
- target-date changes must be independently auditable from generic goal updates

## Intervention Expectations

- interventions may be completed or deactivated independently of the parent goal
- intervention completion alone does not auto-complete a goal without explicit progression logic

## Progress Note Expectations

- progress notes are append-oriented
- amendments must preserve the original note trail
- progress notes may influence projections but must not overwrite historical state

## Care-Plan Sync Expectations

- sync links may report `ALIGNED`, `UNSYNCED`, `STALE`, or `FAILED`
- sync drift may publish progression events without mutating the underlying goal record
- synchronization must preserve branch and patient scoping context
