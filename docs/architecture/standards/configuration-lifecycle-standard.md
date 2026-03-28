# Epic 2 Configuration Lifecycle Standard

Purpose

This standard defines the shared lifecycle, effective-date, and dependency handling rules for Epic 2 configuration entities.

Shared statuses

1. `DRAFT`
   Record is incomplete or unpublished and should not affect live workflows.
2. `ACTIVE`
   Record is usable by current and future workflows.
3. `INACTIVE`
   Record is retained for history and references but should not be used for new work.
4. `ARCHIVED`
   Record is retained for audit/history and is not expected to return to normal use.

Lifecycle rules

- `DRAFT -> ACTIVE` is allowed when the record is complete enough for use
- `ACTIVE -> INACTIVE` is the default “turn off” action
- `INACTIVE -> ACTIVE` is allowed when reactivation is safe
- `INACTIVE -> ARCHIVED` is allowed when a record should be retired permanently
- `ARCHIVED` records should not be reactivated without an explicit exception process

Effective window rules

- use `effective_from` and `effective_to` only where retroactive interpretation matters
- if both values exist, `effective_to` must be greater than or equal to `effective_from`
- a record is currently effective only when:
  - status is `ACTIVE`
  - current time is not before `effective_from`
  - current time is not after `effective_to`

Dependency handling rules

- configuration records with active downstream references should not be hard-deleted
- when a dependency blocks removal, the API should return a controlled conflict response
- dependency conflicts should return HTTP `409 Conflict`
- current enforced examples include:
  - service lines referenced by active visit types or active task templates
  - visit types referenced by active task templates
- the default operator action should be:
  - deactivate
  - archive
  - replace with a new active record

UI/API expectations

- destructive actions must communicate lifecycle consequences clearly
- APIs should return validation errors for invalid effective windows
- APIs should expose enough state for the UI to show whether a record is draft, active, inactive, archived, or future-dated
