# Compliance Lifecycle Standard

## Status

Accepted

## Epic

Epic 11: clinical compliance workspace

## Purpose

This standard defines the shared Epic 11 lifecycle, status, and guardrail rules for compliance entities.

## Checklist lifecycle

Checklist results must use [ComplianceChecklistResultStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceChecklistResultStatus.java).

Allowed values:

- `PASS`
- `FAIL`
- `WARNING`
- `NOT_APPLICABLE`

Rules:

- checklist evaluation must produce an explicit result rather than relying only on missing-data inference
- `WARNING` should be used when the patient remains actionable but needs attention
- `FAIL` should be used when required compliance conditions are not met
- `NOT_APPLICABLE` should only be used when a requirement is explicitly out of scope for the patient context being evaluated

## Acknowledgment lifecycle

Consent and right-to-care acknowledgments must use [ConsentAcknowledgmentStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ConsentAcknowledgmentStatus.java).

Allowed values:

- `ACTIVE`
- `EXPIRED`
- `REVOKED`
- `MISSING`

Rules:

- new or refreshed acknowledgments enter `ACTIVE`
- elapsed validity windows must move acknowledgments into `EXPIRED`
- explicit withdrawal or invalidation must move acknowledgments into `REVOKED`
- missing required acknowledgments must be represented as `MISSING` in compliance evaluation outputs

## Certification-period lifecycle

Certification periods must use [CertificationPeriodStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/CertificationPeriodStatus.java).

Allowed values:

- `CURRENT`
- `UPCOMING_EXPIRY`
- `EXPIRED`
- `MISSING`

Rules:

- valid current windows remain `CURRENT`
- backend-configured expiry thresholds may move a current window into `UPCOMING_EXPIRY`
- ended windows without timely replacement move into `EXPIRED`
- absent required certification windows must be represented as `MISSING`

## Risk-reminder lifecycle

Patient-risk reminders must use [PatientRiskReminderStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/PatientRiskReminderStatus.java).

Allowed values:

- `ACTIVE`
- `RESOLVED`
- `EXPIRED`

Rules:

- triggered reminders enter `ACTIVE`
- authorized remediation may move reminders into `RESOLVED`
- stale reminders may move into `EXPIRED` when the originating condition is no longer current or actionable

## Readiness lifecycle

Patient-level readiness projections must use [ComplianceReadinessStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceReadinessStatus.java).

Allowed values:

- `READY`
- `WARNING`
- `NON_COMPLIANT`
- `UNKNOWN`

Rules:

- `READY` means all required evaluated conditions are satisfied
- `WARNING` means the patient remains operationally manageable but has upcoming or partial compliance risk
- `NON_COMPLIANT` means one or more required conditions are not satisfied
- `UNKNOWN` means the backend lacks enough trustworthy source data to classify readiness safely

## Guardrails

Use [ComplianceOperationGuardrail](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/compliance/foundation/ComplianceOperationGuardrail.java).

Required Epic 11 guardrails:

- patient-scoped and branch-aware operations required
- minimum necessary patient context only
- checklist and requirement codes stable
- acknowledgment and certification windows validated
- reminder and projection recalculation audited
- external event contract append only

## Evaluation and recalculation rules

- recalculation must be explicit and auditable
- source-derived compliance outputs must preserve identifiers and evaluation timestamps needed for later investigation
- recalculation must not silently erase prior reminder or acknowledgment history
- unauthorized recalculation or patient-scope access must fail with controlled errors
