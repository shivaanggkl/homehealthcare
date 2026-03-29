package com.homehealthcare.workforce.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkforceAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordCreated(
            AgencyMembership actorMembership,
            Epic4WorkforceTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic4WorkforceAuditAction.CREATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordUpdated(
            AgencyMembership actorMembership,
            Epic4WorkforceTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic4WorkforceAuditAction.UPDATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordDeactivated(
            AgencyMembership actorMembership,
            Epic4WorkforceTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic4WorkforceAuditAction.DEACTIVATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordArchived(
            AgencyMembership actorMembership,
            Epic4WorkforceTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic4WorkforceAuditAction.ARCHIVED, targetType, targetId, branchId, metadataJson);
    }

    public void recordConflictFlagged(
            AgencyMembership actorMembership,
            Epic4WorkforceTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic4WorkforceAuditAction.CONFLICT_FLAGGED, targetType, targetId, branchId, metadataJson);
    }

    public void recordPerformanceRefreshed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic4WorkforceAuditAction.PERFORMANCE_REFRESHED, Epic4WorkforceTargetType.CAREGIVER_PERFORMANCE_SUMMARY, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic4WorkforceAuditAction action,
            Epic4WorkforceTargetType targetType,
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
