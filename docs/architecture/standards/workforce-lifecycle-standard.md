# Workforce Lifecycle Standard

## Status

Accepted

## Purpose

This standard defines the shared lifecycle and guardrail rules for Epic 4 workforce entities.

## Shared lifecycle states

Epic 4 workforce entities may use these shared lifecycle values where the model fits:

- `ACTIVE`
- `INACTIVE`
- `SUSPENDED`
- `UNSCHEDULABLE`
- `ARCHIVED`

## Lifecycle meanings

### ACTIVE

The caregiver or workforce record is usable for normal operations.

### INACTIVE

The record is retained historically but is not expected to participate in active operations.

### SUSPENDED

The caregiver remains known to the system but is intentionally blocked from normal workforce use
pending review or policy action.

### UNSCHEDULABLE

The caregiver remains active in the organization but must not be treated as available for future
scheduling logic.

### ARCHIVED

The record is retained for audit and history but should not re-enter operational workflows without
an explicit restoration rule.

## Guardrail rules

Epic 4 foundation establishes these shared rules:

- one active caregiver profile per membership within an agency
- overlapping availability conflicts must be prevented or normalized by explicit service rules
- overlapping unavailability conflicts must be prevented or normalized by explicit service rules
- branch-scoped workforce access requires assigned-branch validation
- performance indicators are derived summaries and must not be treated as direct mutable records

## Audit expectations

The following operations are audit-sensitive:

- caregiver record creation
- caregiver record updates
- credential status changes
- availability and unavailability changes
- profile deactivation or archival
- conflict or guardrail-triggering events
- performance refresh events when persisted or published

## Future compatibility

These lifecycle rules are designed to support later Epic 5 scheduling behavior without requiring a
separate workforce-state model.
