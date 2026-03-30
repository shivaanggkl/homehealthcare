# Messaging Domain Standard

## Status

Accepted

## Epic

Epic 9: Messaging and coordination

## Purpose

This standard defines the shared Epic 9 backend vocabulary for secure messaging and operational coordination.

## Core categories

Use [MessagingEntityCategory](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/MessagingEntityCategory.java) for Epic 9 messaging-owned entities.

The approved categories are:

- `COMMUNICATION_THREAD`
- `COMMUNICATION_MESSAGE`
- `THREAD_PARTICIPANT`
- `MESSAGE_READ_RECEIPT`
- `STAFF_GROUP`
- `STAFF_GROUP_MEMBER`
- `BRANCH_BROADCAST`
- `ESCALATION_MARKER`
- `COORDINATION_CONTEXT_LINK`
- `DELIVERY_PROJECTION`

## Thread type vocabulary

Use [MessagingThreadType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/MessagingThreadType.java) for thread classification.

Allowed types:

- `DIRECT_SECURE`
- `PATIENT_COORDINATION`
- `VISIT_COORDINATION`
- `TASK_DISCUSSION`
- `BRANCH_BROADCAST`

Thread type must be explicit and not inferred from nullable foreign keys alone.

## Coordination context vocabulary

Use [CoordinationContextType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/CoordinationContextType.java) for thread-level or summary-level context linkage.

Allowed context types:

- `PATIENT`
- `VISIT`
- `TASK`
- `STAFF_GROUP`
- `BRANCH`

## Delivery and escalation vocabulary

Use [MessagingDeliveryState](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/MessagingDeliveryState.java) for delivery/read semantics.

Allowed values:

- `PENDING`
- `DELIVERED`
- `READ`

Use [MessagingEscalationStatus](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/MessagingEscalationStatus.java) for urgency/escalation semantics.

Allowed values:

- `NORMAL`
- `URGENT`
- `ESCALATED`
- `RESOLVED`

## Privacy and audit rules

- message body content must not be copied into audit metadata
- audit targets may record thread, message, group, broadcast, and escalation identifiers
- participant and context summaries returned to clients must expose only data needed for the authorized screen
- patient-linked threads must not reveal unrelated patient context outside authorized scopes

## Audit taxonomy

Use [Epic9MessagingAuditAction](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/Epic9MessagingAuditAction.java) and [Epic9MessagingTargetType](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/Epic9MessagingTargetType.java).

Required Epic 9 audit actions:

- thread created
- message sent
- participant added
- participant removed
- staff group updated
- branch broadcast sent
- message read
- escalation tagged
- escalation resolved

## Delivery projection contract

Use [UnreadDeliveryProjection](/Users/shiva/Documents/GitHub/homehealthcare/src/main/java/com/homehealthcare/messaging/foundation/UnreadDeliveryProjection.java) for coordinator/inbox summary contracts.

Unread or coordination summaries should expose counts, not raw message content.
