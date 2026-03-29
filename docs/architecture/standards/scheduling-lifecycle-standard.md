# Scheduling Lifecycle Standard

This standard defines the shared lifecycle and guardrail expectations for Epic 5 scheduling entities.

## Visit lifecycle

Epic 5 visit scheduling uses these shared statuses:

- `PLANNED`
- `ASSIGNED`
- `OPEN_SHIFT`
- `RESCHEDULED`
- `CANCELLED`

These states are intentionally limited for the scheduling phase.

Later field-execution and EVV states should extend downstream workflows rather than replace these scheduling meanings.

## Guardrails

Epic 5 scheduling must enforce these shared rules:

- inactive caregivers cannot receive active assignments
- cancelled visits cannot remain actively assigned
- out-of-scope branch users cannot mutate schedule records
- cross-tenant schedule access is always blocked
- overlap checks are required for active assignments
- overtime checks are required where policy enables them
- travel awareness should be evaluated before assignment confirmation when supported
- recurring schedule changes must preserve historical occurrences safely

## Conflict outcomes

Scheduling rule evaluation must return one of:

- `CLEAR`
- `WARNING`
- `BLOCKING`

`WARNING` means the action may still be allowed if product-approved.

`BLOCKING` means the action must not be committed.

## History expectations

Scheduling workflows should preserve before/after operational state for:

- reschedules
- cancellations
- assignment changes
- recurring rule updates affecting future occurrences
