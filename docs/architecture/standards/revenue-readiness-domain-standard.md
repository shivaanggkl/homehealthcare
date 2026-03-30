# Revenue Readiness Domain Standard

## Status

Accepted

## Purpose

This standard defines the shared domain vocabulary for Epic 14 revenue-readiness and basic financial-ops modules.

## Shared categories

Use [RevenueReadinessEntityCategory](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/RevenueReadinessEntityCategory.java) for shared entity or projection categories.

Expected categories:

- revenue-readiness projection
- readiness validation result
- revenue exception flag
- payroll export row
- invoice export row
- authorization-usage snapshot
- payer/service summary projection

## Shared outcome vocabulary

Use [RevenueValidationOutcome](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/RevenueValidationOutcome.java) for deterministic gate results.

Allowed values:

- `PASS`
- `WARNING`
- `FAIL`

Use [RevenueUsagePosture](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/RevenueUsagePosture.java) for authorization-consumption posture.

## Shared audit vocabulary

Use [Epic14RevenueReadinessAuditAction](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/Epic14RevenueReadinessAuditAction.java) and [Epic14RevenueReadinessTargetType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/Epic14RevenueReadinessTargetType.java).

Epic 14 audit coverage must exist for:

- readiness recalculation
- exception-flag changes
- export generation
- authorization-usage refresh
- payer/service summary refresh

## Linkage rules

Epic 14 projections must derive deterministically from existing source records instead of manual overrides.

Expected source inputs include:

- visits and branch context
- patient and payer linkage
- patient authorizations
- documentation submission state
- EVV and signature verification
- review/compliance blocker context where used by readiness gates

## Privacy rule

Epic 14 outputs should expose only the minimum financial and patient context required for operational handoff.
