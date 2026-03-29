# EVV Lifecycle Standard

This standard defines the shared lifecycle and guardrail expectations for Epic 7 visit verification and exceptions.

## Verification lifecycle

Epic 7 EVV uses these shared statuses:

- `PENDING_VERIFICATION`
- `VERIFIED`
- `VERIFIED_WITH_WARNING`
- `EXCEPTION_OPEN`
- `EXCEPTION_ACKNOWLEDGED`
- `MISSED_VISIT_REPORTED`
- `ESCALATED`
- `RESOLVED`

These states describe verification and exception posture, not the broader visit-clinical documentation state.

## Geofence outcomes

Geofence evaluation must return one of:

- `WITHIN_TOLERANCE`
- `OUTSIDE_TOLERANCE_WARNING`
- `OUTSIDE_TOLERANCE_BLOCKED`
- `NOT_EVALUABLE`

## Compliance outcomes

EVV readiness projection must return one of:

- `READY`
- `READY_WITH_WARNING`
- `BLOCKED`
- `MISSED_VISIT`

## Guardrails

Epic 7 EVV must enforce these shared rules:

- only the assigned caregiver may submit own EVV events unless an explicit support workflow exists
- cross-tenant and cross-branch EVV access is always blocked
- duplicate or conflicting clock events require explicit resolution rather than silent overwrite
- geofence evaluations must use approved expected-location context
- missing required signatures must not be silently ignored
- missed visits must create durable exception history
- internal-only device metadata must not leak through standard frontend contracts

## History expectations

Epic 7 workflows should preserve durable history for:

- clock-in and clock-out events
- geofence evaluations
- signature completeness changes
- missed-visit reports
- exception state changes
- supervisor notifications
- escalation creation
