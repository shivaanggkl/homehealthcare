# Messaging Lifecycle Standard

## Status

Accepted

## Epic

Epic 9: Messaging and coordination

## Purpose

This standard defines lifecycle and guardrail rules for Epic 9 messaging entities.

## Thread lifecycle

Threads should support at least:

- active
- archived optional for later phases

Branch broadcasts are a distinct thread type and may also carry their own sent/cancelled or active/expired lifecycle.

## Participant lifecycle

Participants should support:

- added
- removed optional

Historical thread membership must remain reconstructable after participant removal.

## Message lifecycle

Messages are append-only for MVP unless a later ADR defines edit/delete rules.

Read state is handled through read receipts or participant read markers rather than destructive mutation of message content.

## Escalation lifecycle

Escalation status should move through:

- `NORMAL`
- `URGENT`
- `ESCALATED`
- `RESOLVED`

Resolved state must preserve who escalated and who resolved when available.

## Guardrails

Use [MessagingOperationGuardrail](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/MessagingOperationGuardrail.java).

Required guardrails:

- participants must remain in the same tenant
- context links must remain in the same tenant
- branch-broadcast targets must respect branch scope
- cross-branch thread access is blocked
- external channel delivery is out of scope for Epic 9
- message body content is not exported in audit metadata

## Authorization rules

- view/send permissions are not implied by unrelated patient or schedule visibility alone
- patient and visit-linked thread access must still respect branch and tenant boundaries
- staff-group and branch-broadcast management remain admin or leadership actions
- escalation-tag management is a separate permission from general send rights
