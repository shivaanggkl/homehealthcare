# Compliance Domain Standard

## Status

Accepted

## Epic

Epic 11: clinical compliance workspace

## Purpose

This standard defines the shared Epic 11 backend vocabulary for compliance checklists, required documentation tracking, consent acknowledgments, certification awareness, patient risk reminders, and patient-level readiness projections.

## Core categories

Use [ComplianceEntityCategory](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceEntityCategory.java) for Epic 11 compliance-owned entities.

The approved categories are:

- `COMPLIANCE_PROFILE`
- `COMPLIANCE_CHECKLIST_DEFINITION`
- `COMPLIANCE_CHECKLIST_RESULT`
- `REQUIRED_DOCUMENTATION_REQUIREMENT`
- `CONSENT_ACKNOWLEDGMENT_RECORD`
- `CERTIFICATION_PERIOD_RECORD`
- `PATIENT_RISK_REMINDER`
- `COMPLIANCE_STATUS_PROJECTION`

## Shared status vocabulary

Checklist results must use [ComplianceChecklistResultStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceChecklistResultStatus.java).

Allowed values:

- `PASS`
- `FAIL`
- `WARNING`
- `NOT_APPLICABLE`

Consent and right-to-care acknowledgment status must use [ConsentAcknowledgmentStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ConsentAcknowledgmentStatus.java).

Allowed values:

- `ACTIVE`
- `EXPIRED`
- `REVOKED`
- `MISSING`

Certification-period status must use [CertificationPeriodStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/CertificationPeriodStatus.java).

Allowed values:

- `CURRENT`
- `UPCOMING_EXPIRY`
- `EXPIRED`
- `MISSING`

Patient-risk reminders must use [PatientRiskReminderStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/PatientRiskReminderStatus.java).

Allowed values:

- `ACTIVE`
- `RESOLVED`
- `EXPIRED`

Patient-level compliance readiness projections must use [ComplianceReadinessStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceReadinessStatus.java).

Allowed values:

- `READY`
- `WARNING`
- `NON_COMPLIANT`
- `UNKNOWN`

## Event vocabulary

Use [ComplianceEventType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceEventType.java) for backend-safe cross-module compliance triggers.

Required Epic 11 event types:

- `PATIENT_RISK_REMINDER_TRIGGERED`
- `PATIENT_RISK_REMINDER_RESOLVED`
- `ACKNOWLEDGMENT_MISSING`
- `ACKNOWLEDGMENT_EXPIRED`
- `CERTIFICATION_PERIOD_EXPIRING`
- `REQUIRED_DOCUMENTATION_GAP_DETECTED`
- `COMPLIANCE_STATUS_CHANGED`

These events are integration-safe signals.

They must capture stable identifiers and compact facts rather than full patient chart content.

## Source-linkage rules

Epic 11 may derive status from earlier epic data, including:

- patient records and payer/authorization context from Epic 3
- scheduling and visit-occurrence timing from Epic 5
- mobile execution and submitted field artifacts from Epic 6
- EVV and missed-visit/exception state from Epic 7
- submitted documentation and attachment links from Epic 8
- review outcomes and return-for-fix signals from Epic 10

Epic 11 compliance entities must preserve source record identifiers and evaluation timestamps instead of duplicating full source payloads.

## Privacy and audit rules

- compliance dashboard and patient-level summaries must expose only the minimum patient and branch context needed for authorized users
- checklist definitions, requirement codes, and reminder codes must stay stable enough for audit and analytics use
- patient-facing form content and narrative source documents must not be copied wholesale into Epic 11 audit metadata
- compliance projections must explain warning and non-compliant conditions with structured codes rather than only narrative text

## Audit taxonomy

Use [Epic11ComplianceAuditAction](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/Epic11ComplianceAuditAction.java) and [Epic11ComplianceTargetType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/Epic11ComplianceTargetType.java).

Required Epic 11 audit actions:

- compliance checklist definition saved
- compliance checklist result recalculated
- documentation requirement saved
- acknowledgment recorded
- acknowledgment revoked
- certification period saved
- patient risk reminder saved
- patient risk reminder resolved
- compliance status projection recalculated
