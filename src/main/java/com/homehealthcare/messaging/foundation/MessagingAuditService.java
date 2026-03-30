package com.homehealthcare.messaging.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessagingAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordThreadCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.THREAD_CREATED, Epic9MessagingTargetType.COMMUNICATION_THREAD, targetId, branchId, metadataJson);
    }

    public void recordMessageSent(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.MESSAGE_SENT, Epic9MessagingTargetType.COMMUNICATION_MESSAGE, targetId, branchId, metadataJson);
    }

    public void recordParticipantAdded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.PARTICIPANT_ADDED, Epic9MessagingTargetType.THREAD_PARTICIPANT, targetId, branchId, metadataJson);
    }

    public void recordStaffGroupUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.STAFF_GROUP_UPDATED, Epic9MessagingTargetType.STAFF_GROUP, targetId, branchId, metadataJson);
    }

    public void recordBranchBroadcastSent(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.BRANCH_BROADCAST_SENT, Epic9MessagingTargetType.BRANCH_BROADCAST, targetId, branchId, metadataJson);
    }

    public void recordMessageRead(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.MESSAGE_READ, Epic9MessagingTargetType.COMMUNICATION_MESSAGE, targetId, branchId, metadataJson);
    }

    public void recordEscalationTagged(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.ESCALATION_TAGGED, Epic9MessagingTargetType.ESCALATION_MARKER, targetId, branchId, metadataJson);
    }

    public void recordEscalationResolved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic9MessagingAuditAction.ESCALATION_RESOLVED, Epic9MessagingTargetType.ESCALATION_MARKER, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic9MessagingAuditAction action,
            Epic9MessagingTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        Objects.requireNonNull(actorMembership, "actorMembership must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");

        auditEventRepository.save(AuditEvent.createSuccess(
                ACTOR_TYPE,
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                action.actionType(),
                targetType.name(),
                targetId,
                actorMembership.getAgencyId(),
                branchId,
                metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson));
    }
}
