# EVV Domain Standard

This standard defines the shared backend rules for Epic 7 visit-execution verification and exception entities.

## EVV categories

Epic 7 entities must fit one of these categories:

- `EVV_VERIFICATION_SESSION`
- `EVV_CLOCK_EVENT`
- `DEVICE_METADATA_SNAPSHOT`
- `GEOFENCE_TOLERANCE_RULE`
- `GEOFENCE_EVALUATION`
- `SIGNATURE_VERIFICATION_LINK`
- `MISSED_VISIT_RECORD`
- `VISIT_EXCEPTION_RECORD`
- `SUPERVISOR_NOTIFICATION_EVENT`
- `ESCALATION_REQUEST`
- `EVV_COMPLIANCE_PROJECTION`

## Shared modeling rules

- EVV records are always agency-owned.
- Visit verification records must reference Epic 5 visits rather than inventing parallel visit identities.
- EVV records must link back to the assigned caregiver or authorized operational actor.
- EVV contracts should reuse Epic 6 mobile execution context where practical rather than duplicating the entire field-session model.
- Device and location data must be normalized and minimized for privacy.
- Frontend-facing EVV contracts must expose outcomes and reason codes, not raw internal policy internals.

## Phase A shared contracts

Phase A establishes these shared contracts:

- geofence evaluation result:
  - outcome
  - distance from expected meters
  - tolerance meters used
  - blocking flag
  - reason code
- EVV compliance projection:
  - start event present
  - end event present
  - geofence outcome
  - signature complete
  - open exception count
  - missed-visit reported
  - overall outcome

## Audit expectations

Epic 7 actions that change verification or exception state must be auditable, including:

- EVV clock-in
- EVV clock-out
- geofence evaluation
- signature status recording
- missed-visit reporting
- EVV exception logging
- supervisor notification
- escalation creation

## Scope boundary

Epic 7 EVV domain does not yet implement:

- the broader Epic 8 note-template/form domain
- generalized messaging expansion from Epic 9
- QA queue ownership and review assignment from Epic 10
- claims and billing readiness workflows from later epics
