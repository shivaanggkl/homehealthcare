# Epic 3 Patient Lifecycle Standard

Purpose

This standard defines the shared lifecycle, duplicate-handling, archival, and attachment guardrail rules for Epic 3 patient entities.

Shared statuses

1. `ACTIVE`
   Record is usable by current and future workflows.
2. `INACTIVE`
   Record is retained for history and references but should not be used for new work.
3. `ARCHIVED`
   Record is retained for audit/history and is not expected to return to normal use.
4. `DUPLICATE_CANDIDATE`
   Record requires duplicate review before it can continue as a normal active record.
5. `DUPLICATE_MERGED`
   Record has been merged into a canonical patient and is retained for history and traceability.

Lifecycle rules

- `ACTIVE -> INACTIVE` is the default “turn off” action
- `INACTIVE -> ACTIVE` is allowed when reactivation is safe
- `ACTIVE -> DUPLICATE_CANDIDATE` is allowed when a likely duplicate is detected
- `DUPLICATE_CANDIDATE -> ACTIVE` is allowed when duplicate review dismisses the conflict
- `DUPLICATE_CANDIDATE -> DUPLICATE_MERGED` is allowed only when merge handling preserves traceability
- `INACTIVE -> ARCHIVED` is allowed when a record should be retired permanently
- `ARCHIVED` and `DUPLICATE_MERGED` records should not return to normal use without an explicit exception process

Duplicate-handling rules

- duplicate detection should happen before destructive merge logic
- candidate duplicates must be reviewable and auditable
- merge outcomes must preserve canonical-record linkage and audit trail
- UI and API behavior should use controlled conflict or review-required responses rather than silent merges

Attachment guardrail rules

- attachment metadata must remain linked to an authorized patient record
- upload and download actions require explicit permission checks
- storage keys or provider-internal references must not be exposed to end users
- attachment records should prefer inactive or archived retention over destructive deletion where compliance or audit value exists

Deletion and archival rules

- hard delete is not the default operator action
- records with active authorizations, payer links, or attachments should not be hard-deleted
- when a dependency blocks removal, the API should return a controlled conflict response
- operators should prefer:
  - deactivate
  - archive
  - mark duplicate and resolve through a controlled review path
