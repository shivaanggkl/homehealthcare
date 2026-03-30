# Patient Event Domain Standard

## Status

Accepted

## Epic

Epic 12: incident, infection, and wound workflows

## Purpose

This standard defines the shared Epic 12 backend vocabulary for patient events, including incidents, infections, wounds, evidence linkage, follow-up ownership, escalation, alert events, and longitudinal patient history.

## Core categories

Use [PatientEventEntityCategory](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventEntityCategory.java) for Epic 12 patient-event-owned entities.

The approved categories are:

- `INCIDENT_RECORD`
- `INFECTION_RECORD`
- `WOUND_RECORD`
- `PATIENT_EVENT_EVIDENCE_LINK`
- `PATIENT_EVENT_FOLLOW_UP_ASSIGNMENT`
- `PATIENT_EVENT_ESCALATION_RECORD`
- `PATIENT_EVENT_ALERT_EVENT`
- `PATIENT_EVENT_HISTORY_ENTRY`

## Shared status vocabulary

Incident lifecycle must use [IncidentRecordStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/IncidentRecordStatus.java).

Allowed values:

- `OPEN`
- `IN_REVIEW`
- `RESOLVED`
- `CLOSED`

Infection lifecycle must use [InfectionRecordStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/InfectionRecordStatus.java).

Allowed values:

- `ACTIVE`
- `MONITORING`
- `RESOLVED`

Wound lifecycle must use [WoundRecordStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/WoundRecordStatus.java).

Allowed values:

- `ACTIVE`
- `MONITORING`
- `IMPROVING`
- `STABLE`
- `DETERIORATING`
- `RESOLVED`

Follow-up lifecycle must use [PatientEventFollowUpStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventFollowUpStatus.java).

Allowed values:

- `OPEN`
- `COMPLETED`
- `CANCELLED`

Escalation lifecycle must use [PatientEventEscalationStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventEscalationStatus.java).

Allowed values:

- `ACTIVE`
- `CLEARED`

Longitudinal history typing must use [PatientEventHistoryEntryType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventHistoryEntryType.java).

Required Epic 12 types:

- `INCIDENT_EVENT`
- `INFECTION_EVENT`
- `WOUND_CREATED`
- `WOUND_HISTORY_CAPTURED`
- `FOLLOW_UP_MILESTONE`
- `ESCALATION_MILESTONE`
- `EVIDENCE_EVENT`

## Alert-event contract

Use [PatientEventAlertType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventAlertType.java) and [PatientEventAlertContract](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/PatientEventAlertContract.java) for backend-safe Epic 12 alert publication.

Required Epic 12 alert types:

- `HIGH_SEVERITY_INCIDENT`
- `OVERDUE_FOLLOW_UP`
- `ACTIVE_INFECTION`
- `WOUND_PROGRESSION_ALERT`

Alert contracts must carry stable identifiers and compact summaries rather than full patient-event narrative content.

## Source-linkage rules

Epic 12 may link to earlier epic data, including:

- patient records and patient attachments from Epic 3
- visit and branch scheduling context from Epic 5
- mobile field artifacts and field incidents from Epic 6
- EVV exception context from Epic 7
- documentation records and attachment links from Epic 8
- messaging/escalation coordination from Epic 9
- compliance reminders and review follow-up from Epics 10 and 11

Epic 12 entities must preserve source record identifiers and timestamps instead of copying full source payloads.

## Privacy and audit rules

- patient-event routes must expose only the minimum patient and visit context needed for authorized workflows
- evidence links must reference attachment systems by stable identifiers without leaking storage internals
- narratives, wound observations, and infection summaries must not be copied wholesale into audit metadata
- history projections must explain event type and timing with stable codes rather than only free text

## Audit taxonomy

Use [Epic12PatientEventAuditAction](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/Epic12PatientEventAuditAction.java) and [Epic12PatientEventTargetType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/patientevent/foundation/Epic12PatientEventTargetType.java).

Required Epic 12 audit actions:

- incident created
- incident updated
- infection created
- infection updated
- wound created
- wound history added
- evidence linked
- follow-up assigned
- escalation created
- escalation cleared
- patient-event record resolved
