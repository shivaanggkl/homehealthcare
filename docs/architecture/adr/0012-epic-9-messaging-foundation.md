# ADR 0012: Epic 9 Messaging Foundation

## Status

Accepted

## Context

Epic 9 introduces secure in-app messaging and operational coordination across patient, visit, task, group, and branch contexts.

The platform already has audit, tenant isolation, branch-aware authorization, patient/workforce/scheduling context, and caregiver mobile messaging expectations from earlier epics.

Epic 9 needs a shared foundation before thread, message, and broadcast APIs can be built safely.

## Decision

Epic 9 uses a single messaging foundation with:

- explicit communication thread and message categories
- explicit thread types for direct, patient, visit, task, and branch-broadcast contexts
- explicit delivery and escalation vocabularies
- shared audit taxonomy for secure coordination operations
- explicit permissions for messaging workspace visibility, secure send, staff-group management, branch broadcasts, and escalation-tag management

Audit metadata for Epic 9 must stay content-safe.

Message body content is not treated as export-safe audit metadata.

## Consequences

- web and mobile clients can build on one messaging vocabulary
- patient, visit, and task-linked communication stays explicit instead of being inferred ad hoc
- later notification fanout can attach to stable delivery and unread-state contracts
- coordinator-facing summaries can be added without redefining core audit or permission models

## Follow-up

Phase B will add the concrete domain entities and persistence model.

Phase C will add the public APIs for inbox, threads, groups, broadcasts, and escalation actions.
