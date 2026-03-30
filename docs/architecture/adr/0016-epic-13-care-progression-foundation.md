# ADR 0016: Epic 13 Care-Progression Foundation

## Status

Accepted

## Context

Epic 13 introduces goal templates, patient goals, interventions, progress notes, version history, and care-plan synchronization. These records sit across patient, documentation, scheduling, compliance, and review contexts, so the platform needs a single foundation for lifecycle, authorization, and audit expectations before feature-specific persistence starts.

## Decision

Adopt a dedicated `careprogression` domain foundation with:

- explicit shared entity categories for templates, patient goals, interventions, progress notes, versions, sync links, projections, and progression events
- explicit lifecycle vocabularies for templates, goals, interventions, notes, and care-plan sync
- explicit progression-event vocabulary for state changes, overdue/at-risk target dates, and sync drift
- explicit Epic 13 audit actions and target types wired into the existing audit-event infrastructure
- explicit Epic 13 permission matrix entries for view, template management, patient-goal management, intervention management, progress-note entry, state transitions, and care-plan sync

## Consequences

- later Epic 13 persistence and APIs can reuse a stable vocabulary instead of redefining states ad hoc
- goal and intervention workflows remain branch-aware and tenant-safe by default
- later dashboards and review/compliance integrations can build on the shared progression-event contract
- Epic 13 can evolve independently from Epic 8 documentation and Epic 11 compliance without duplicating their models
