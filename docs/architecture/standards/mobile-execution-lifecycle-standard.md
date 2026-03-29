# Mobile Execution Lifecycle Standard

This standard defines the shared lifecycle and guardrail expectations for Epic 6 caregiver mobile execution.

## Visit execution lifecycle

Epic 6 mobile visit execution uses these shared statuses:

- `NOT_STARTED`
- `IN_PROGRESS`
- `PAUSED`
- `COMPLETED`
- `FAILED_SYNC`

These states describe caregiver-facing execution progress only.

Later EVV and exception workflows should extend downstream logic rather than replace these mobile meanings.

## Sync outcomes

Offline sync handling must return one of:

- `ACCEPTED`
- `DUPLICATE_ALREADY_APPLIED`
- `REJECTED_VALIDATION`
- `REJECTED_AUTHORIZATION`

These outcomes are intended to support retry-safe field workflows under intermittent connectivity.

## Guardrails

Epic 6 mobile execution must enforce these shared rules:

- only the assigned caregiver can execute a visit unless an explicit elevated-support workflow exists
- only one active execution session may exist per visit at a time
- completed visits cannot be silently reopened
- offline mutations must be idempotent
- cross-tenant and cross-caregiver mobile access is always blocked
- mobile payloads must exclude admin-only operational fields
- signature and photo uploads must follow controlled storage restrictions

## History expectations

Mobile workflows should preserve durable history for:

- visit execution start and end
- task completion updates
- quick note saves
- photo and signature artifact creation
- incident creation
- offline sync acceptance and rejection when tracked operationally
