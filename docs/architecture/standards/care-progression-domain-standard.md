# Care-Progression Domain Standard

## Purpose

This standard defines the shared Epic 13 vocabulary for goal templates, patient goals, interventions, progress notes, version history, care-plan synchronization, and progression events.

## Shared Entity Categories

- `GOAL_TEMPLATE`
- `PATIENT_GOAL`
- `GOAL_INTERVENTION`
- `GOAL_PROGRESS_NOTE`
- `GOAL_VERSION`
- `INTERVENTION_VERSION`
- `CAREPLAN_SYNC_LINK`
- `GOAL_STATUS_PROJECTION`
- `PROGRESSION_EVENT`

## Linkage Rules

- patient goals are patient-scoped and may reference a source goal template
- interventions are always attached to one patient goal
- progress notes attach to one patient goal and may optionally reference one intervention
- goal versions are append-oriented historical records tied to one patient goal
- care-plan sync links attach to one patient goal and one external or internal care-plan identifier
- progression projections are derived records and must not become the system of record for mutable goal data
- visit, documentation, review, and compliance contexts may reference patient goals or interventions, but Epic 13 remains the source of truth for goal progression state

## Shared Lifecycle Vocabulary

- templates use `DRAFT`, `ACTIVE`, `INACTIVE`, `ARCHIVED`
- patient goals use `ACTIVE`, `COMPLETED`, `UNMET`, `NOT_ATTAINED`, `CANCELLED`
- interventions use `ACTIVE`, `COMPLETED`, `INACTIVE`, `CANCELLED`
- progress notes use `DRAFT`, `FINALIZED`, `AMENDED`
- care-plan sync uses `ALIGNED`, `UNSYNCED`, `STALE`, `FAILED`

## Shared Projection And Event Vocabulary

- target-date posture may use `ON_TRACK`, `AT_RISK`, `OVERDUE`, `NOT_APPLICABLE`
- progression events may use:
  - `GOAL_STATE_CHANGED`
  - `TARGET_DATE_AT_RISK`
  - `TARGET_DATE_OVERDUE`
  - `CAREPLAN_SYNC_DRIFT_DETECTED`

## Audit Expectations

Epic 13 audit coverage must support at least:

- goal template save
- patient goal create/update
- intervention save
- progress note add
- goal state change
- target date change
- goal version record
- care-plan sync update
- progression event publication
