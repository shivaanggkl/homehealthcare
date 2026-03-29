# Mobile Execution Domain Standard

This standard defines the shared backend rules for Epic 6 caregiver mobile entities.

## Mobile execution categories

Epic 6 entities must fit one of these categories:

- `MOBILE_DEVICE_SESSION`
- `TODAY_WORK_QUERY`
- `VISIT_EXECUTION_SESSION`
- `ROUTE_STOP_PROJECTION`
- `PATIENT_SUMMARY_SNAPSHOT`
- `CARE_INSTRUCTION_SNAPSHOT`
- `TASK_CHECKLIST_ENTRY`
- `QUICK_NOTE_ENTRY`
- `FIELD_PHOTO_ARTIFACT`
- `FIELD_SIGNATURE_ARTIFACT`
- `INCIDENT_REPORT`
- `MESSAGE_THREAD_SUMMARY`
- `OFFLINE_SYNC_ENVELOPE`

## Shared modeling rules

- Mobile execution records are always agency-owned.
- Field work must reference Epic 5 visits rather than duplicate independent mobile visit identities.
- Mobile responses must expose only caregiver-safe patient and operational context.
- Mobile contracts must not leak admin-only workflow or scheduling internals.
- Offline-tolerant mobile mutations must be representable by an idempotent sync envelope.

## Mobile auth and sync contracts

Phase A establishes these shared contracts:

- mobile session context:
  - user id
  - agency id
  - membership id
  - caregiver profile id optional in foundation
  - branch id optional
  - session id
  - granted permissions
  - offline sync enabled flag
- offline sync envelope:
  - envelope id
  - operation type
  - idempotency key
  - client occurred at
  - visit id optional by operation
  - caregiver profile id optional by operation
  - payload checksum optional
- offline sync result:
  - `ACCEPTED`
  - `DUPLICATE_ALREADY_APPLIED`
  - `REJECTED_VALIDATION`
  - `REJECTED_AUTHORIZATION`

## Audit expectations

Mobile actions that change field-execution state must be auditable, including:

- mobile session bootstrap when treated as an auditable support event
- visit execution start
- visit execution end
- task checklist save
- quick note save
- photo upload
- signature capture
- incident flagging
- mobile message send
- offline sync acceptance or rejection when surfaced as an auditable event

## Scope boundary

Epic 6 mobile execution domain does not yet implement:

- EVV geofence tolerance rules
- missed-visit workflows
- supervisor exception routing
- QA completeness review
- revenue-readiness checks
