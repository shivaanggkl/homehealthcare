# ADR 0014: Epic 11 Compliance Foundation

## Status

Accepted

## Context

Epic 11 introduces a dedicated clinical compliance workspace for care-plan completeness, required documentation tracking, consent and right-to-care acknowledgments, certification-period awareness, patient risk reminders, and patient-level compliance status visibility.

Earlier epics already provide patient context, scheduling, mobile execution, EVV, documentation, review, and audit infrastructure. Epic 11 needs a shared compliance foundation before checklist, reminder, and dashboard persistence can be built safely.

## Decision

Epic 11 uses a single compliance foundation with:

- explicit compliance-owned entity categories
- explicit status vocabulary for checklist results, consent acknowledgments, certification periods, risk reminders, and patient readiness projections
- shared event vocabulary for reminder triggers, expirations, missing acknowledgments, documentation gaps, and compliance status changes
- shared audit taxonomy for checklist-definition changes, requirement updates, acknowledgment actions, certification updates, reminder lifecycle actions, and readiness recalculation
- explicit permissions for compliance workspace visibility, dashboard visibility, checklist management, documentation-rule management, acknowledgment management, certification-period management, patient-risk reminder management, and readiness recalculation

Audit metadata for Epic 11 must stay privacy-aware.

Patient-facing form content, source-document contents, and full clinical narratives are not treated as export-safe audit metadata.

## Consequences

- later Epic 11 services can build on one compliance vocabulary instead of inventing separate lifecycle rules per checklist or reminder type
- patient, documentation, EVV, and review signals can contribute to compliance status through stable event and readiness contracts
- branch-aware authorization remains explicit for compliance dashboard visibility and patient-level compliance actions
- the existing audit API can expose Epic 11 compliance events without any new audit transport

## Follow-up

Phase B will add the concrete checklist, acknowledgment, certification, reminder, and compliance-status persistence model.

Phase C will add the public APIs for compliance dashboards, patient-level compliance detail, checklist management, and reminder workflows.
