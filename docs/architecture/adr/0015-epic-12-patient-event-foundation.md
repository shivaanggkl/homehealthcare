# ADR 0015: Epic 12 Patient-Event Foundation

## Status

Accepted

## Context

Epic 12 introduces structured incident, infection, and wound workflows with attachment evidence, follow-up ownership, escalation, alert signals, and longitudinal patient history.

Earlier epics already provide patient context, scheduling, mobile artifacts, documentation, messaging, compliance, audit, and branch-aware authorization. Epic 12 needs a shared patient-event foundation before incident, infection, wound, and follow-up persistence can be built safely.

## Decision

Epic 12 uses a single patient-event foundation with:

- explicit patient-event-owned entity categories
- shared status vocabulary for incidents, infections, wounds, follow-up assignments, and escalations
- explicit history-entry and alert-event vocabulary
- shared audit taxonomy for incident, infection, wound, evidence, follow-up, escalation, and resolution events
- explicit permissions for workspace visibility, incident creation, infection and wound management, evidence linkage, follow-up assignment, escalation, and resolution

Audit metadata for Epic 12 must stay privacy-aware.

Patient narratives, photo contents, and detailed clinical observations are not treated as export-safe audit metadata.

## Consequences

- later Epic 12 services can build on one lifecycle vocabulary instead of inventing separate operational rules per record type
- patient, visit, mobile-artifact, documentation, and messaging context can link into Epic 12 through stable identifiers instead of copied payloads
- branch-aware authorization remains explicit for sensitive patient-event visibility and mutation actions
- the existing audit API can expose Epic 12 patient-event events without any new audit transport

## Follow-up

Phase B will add the concrete incident, infection, wound, evidence, follow-up, escalation, and longitudinal-history persistence model.

Phase C will add the public APIs for patient-event record management, evidence/follow-up/escalation workflows, and patient-event timelines.
