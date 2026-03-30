# Patient Event Lifecycle Standard

## Status

Accepted

## Epic

Epic 12: incident, infection, and wound workflows

## Purpose

This standard defines the shared lifecycle, alerting, and guardrail rules for Epic 12 patient-event entities.

## Lifecycle rules

### Incident records

Incident records must use [IncidentRecordStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/IncidentRecordStatus.java).

Rules:

- incidents begin in `OPEN`
- `IN_REVIEW` indicates triage or active operational review
- `RESOLVED` indicates the event has been addressed but should still remain visible in history
- `CLOSED` is terminal and should only happen after follow-up or escalation state is settled

### Infection records

Infection records must use [InfectionRecordStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/InfectionRecordStatus.java).

Rules:

- infections begin as `ACTIVE` or `MONITORING`
- infections may move from `ACTIVE` to `MONITORING`
- `RESOLVED` is terminal for the active infection record, but history remains queryable

### Wound records

Wound records must use [WoundRecordStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/WoundRecordStatus.java).

Rules:

- wounds begin in `ACTIVE` or `MONITORING`
- progression states such as `IMPROVING`, `STABLE`, and `DETERIORATING` must preserve a longitudinal trail through history entries
- `RESOLVED` closes the active wound state but does not remove wound history

### Follow-up assignments

Follow-up records must use [PatientEventFollowUpStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventFollowUpStatus.java).

Rules:

- follow-up begins in `OPEN`
- `COMPLETED` is terminal for normal follow-up closure
- `CANCELLED` is terminal and must remain auditable

### Escalations

Escalation records must use [PatientEventEscalationStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventEscalationStatus.java).

Rules:

- escalations begin in `ACTIVE`
- `CLEARED` is terminal and must preserve who cleared it and when

## Longitudinal history rules

Epic 12 longitudinal history must use [PatientEventHistoryEntryType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventHistoryEntryType.java).

Rules:

- history is append-only
- history ordering is chronological by source event timestamp
- history entries must link to the source record instead of copying full source payloads

## Alert rules

Epic 12 alert publication must use [PatientEventAlertType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventAlertType.java).

Rules:

- high-severity incidents may emit `HIGH_SEVERITY_INCIDENT`
- open overdue follow-up may emit `OVERDUE_FOLLOW_UP`
- active infections may emit `ACTIVE_INFECTION`
- wound deterioration or material wound change may emit `WOUND_PROGRESSION_ALERT`

Alert contracts are append-only and must not contain raw attachment contents or full clinical narratives.

## Guardrails

Use [PatientEventOperationGuardrail](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventOperationGuardrail.java).

Required guardrails:

- `PATIENT_AND_BRANCH_SCOPED`
- `MINIMUM_NECESSARY_PATIENT_CONTEXT_ONLY`
- `EVIDENCE_LINKS_REFERENCE_EXISTING_ATTACHMENT_STORES`
- `FOLLOW_UP_AND_ESCALATION_CHANGES_AUDITED`
- `LONGITUDINAL_HISTORY_APPEND_ONLY`
- `ALERT_EVENT_TYPES_STABLE`
