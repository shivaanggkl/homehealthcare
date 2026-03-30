# Revenue Readiness Lifecycle Standard

## Status

Accepted

## Purpose

This standard defines the shared lifecycle and guardrail rules for Epic 14 revenue-readiness modules.

## Readiness lifecycle

Use [RevenueReadinessStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/RevenueReadinessStatus.java).

Allowed values:

- `READY`
- `WARNING`
- `BLOCKED`
- `EXPORTED`

Rules:

- `READY` means required gates passed for the configured export purpose
- `WARNING` means export is possible but flagged
- `BLOCKED` means one or more required gates failed
- `EXPORTED` means output was generated and handed off through the approved export lifecycle

## Export lifecycle

Use [RevenueExportLifecycleStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/RevenueExportLifecycleStatus.java).

Allowed values:

- `NOT_REQUESTED`
- `STAGED`
- `GENERATED`
- `HANDED_OFF`
- `VOIDED`

## Guardrails

Use [RevenueOperationGuardrail](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/revenuereadiness/foundation/RevenueOperationGuardrail.java).

Epic 14 workflows must follow these rules:

- readiness must remain branch-aware and tenant-scoped
- recalculation must be repeatable from source records
- export generation must be explicitly controlled and audited
- revenue outputs must not expose unnecessary patient or internal-only metadata
- Epic 14 must stop at readiness and export handoff, not full claims processing
