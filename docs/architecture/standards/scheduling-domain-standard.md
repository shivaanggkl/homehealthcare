# Scheduling Domain Standard

This standard defines the shared backend rules for Epic 5 scheduling entities.

## Scheduling categories

Epic 5 scheduling entities must fit one of these categories:

- `VISIT_OCCURRENCE`
- `RECURRING_VISIT_RULE`
- `CAREGIVER_ASSIGNMENT`
- `OPEN_SHIFT`
- `RESCHEDULE_EVENT`
- `CANCELLATION_EVENT`
- `MATCH_EVALUATION`
- `CONFLICT_EVALUATION`
- `TRAVEL_AWARENESS`

## Shared modeling rules

- Scheduling records are always agency-owned.
- Branch linkage is explicit when the patient, caregiver, or policy requires branch scoping.
- Scheduling records should reference Epic 3 patients and Epic 4 caregivers rather than duplicate patient or caregiver profile fields.
- Scheduling actions must preserve history instead of silently overwriting operational state.
- Public scheduling APIs may expose structured warnings and conflicts, but must not leak internal-only scoring or routing internals.

## Travel-awareness contract

Phase A establishes a public scheduling travel contract:

- estimated travel minutes
- available gap minutes
- evaluation level:
  - `FEASIBLE`
  - `TIGHT_CONNECTION`
  - `INFEASIBLE`
  - `UNKNOWN`
- rationale code

This contract is intended for assignment previews, reschedule previews, and schedule warnings.

## Audit expectations

Scheduling actions that change operational state must be audited, including:

- visit creation
- visit update
- caregiver assignment
- assignment removal
- open-shift creation
- open-shift closure
- visit reschedule
- visit cancellation
- conflict flagging
- travel evaluation when surfaced as an auditable scheduling action

## Scope boundary

Epic 5 scheduling domain does not include:

- EVV
- clock-in / clock-out
- GPS proof
- signatures
- visit note completion
- billing readiness
