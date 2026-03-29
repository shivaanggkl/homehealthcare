# Workforce Domain Standard

## Status

Accepted

## Purpose

This standard defines shared categories, fields, and audit expectations for Epic 4 workforce
modules.

It exists so caregiver workforce entities are modeled consistently before scheduling logic is added
in later epics.

## Shared entity categories

Epic 4 workforce entities are grouped into these categories:

- `CAREGIVER_PROFILE`
- `CREDENTIAL_LICENSURE`
- `LANGUAGE_COMMUNICATION`
- `SKILL_CAPABILITY`
- `GEOGRAPHY_PREFERENCE`
- `SHIFT_PREFERENCE`
- `AVAILABILITY_WINDOW`
- `UNAVAILABILITY_WINDOW`
- `PERFORMANCE_INDICATOR`

## Shared ownership rules

- workforce records are agency-owned by default
- branch association is allowed where workforce visibility or schedulability is branch-limited
- workforce records must not bypass agency membership ownership

## Shared field expectations

### Caregiver profile

Expected base fields:

- `agency_id`
- `user_id` or membership linkage
- `status`
- optional workforce reference code
- optional branch association
- optional employment and start or end metadata

### Credentials and licensure

Expected fields:

- credential or certification type
- status
- issued date optional
- expiry date optional
- verification state optional

### Language, skills, geography, and preferences

Expected fields vary by entity, but each record must:

- identify the caregiver profile
- define the actual capability or preference value
- support optional notes
- remain audit-sensitive

### Availability and unavailability

Expected fields:

- caregiver profile linkage
- time window or recurring pattern
- reason or type when needed
- optional branch or operational scope

### Performance indicators

Expected Epic 4 indicators are:

- `COMPLETED_VISITS_COUNT`
- `MISSED_VISITS_COUNT`
- `ON_TIME_PERCENTAGE`
- `DOCUMENTATION_COMPLETION_PERCENTAGE`
- `EXCEPTION_COUNT`

These are derived summaries, not direct mutable business records.

## Audit standard

All Epic 4 workforce changes must use standardized audit actions:

- `WORKFORCE_RECORD_CREATED`
- `WORKFORCE_RECORD_UPDATED`
- `WORKFORCE_RECORD_DEACTIVATED`
- `WORKFORCE_RECORD_ARCHIVED`
- `WORKFORCE_CONFLICT_FLAGGED`
- `WORKFORCE_PERFORMANCE_REFRESHED`

All Epic 4 audit targets must use standardized target types defined in
`Epic4WorkforceTargetType`.

## Security standard

- workforce visibility and mutation must use workforce-specific permissions
- branch-scoped roles must not operate outside assigned branches
- performance visibility must not imply workforce mutation rights

## Non-goals for Epic 4

This standard does not authorize:

- drag-and-drop scheduling logic
- route optimization
- visit assignment engines
- EVV execution details

Those belong to later epics.
