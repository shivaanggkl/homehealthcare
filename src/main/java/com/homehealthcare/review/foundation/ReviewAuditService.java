package com.homehealthcare.review.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordReviewItemCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.REVIEW_ITEM_CREATED, Epic10ReviewTargetType.REVIEW_WORK_ITEM, targetId, branchId, metadataJson);
    }

    public void recordReviewAssigned(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.REVIEW_ASSIGNED, Epic10ReviewTargetType.REVIEW_ASSIGNMENT, targetId, branchId, metadataJson);
    }

    public void recordReviewReassigned(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.REVIEW_REASSIGNED, Epic10ReviewTargetType.REVIEW_ASSIGNMENT, targetId, branchId, metadataJson);
    }

    public void recordReviewDecision(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.REVIEW_DECISION_RECORDED, Epic10ReviewTargetType.REVIEW_DECISION, targetId, branchId, metadataJson);
    }

    public void recordReturnedForFix(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.REVIEW_RETURNED_FOR_FIX, Epic10ReviewTargetType.REVIEW_DECISION, targetId, branchId, metadataJson);
    }

    public void recordSignoffRequested(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.SIGNOFF_REQUESTED, Epic10ReviewTargetType.SIGNOFF_REQUEST, targetId, branchId, metadataJson);
    }

    public void recordCompletenessRecalculated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic10ReviewAuditAction.COMPLETENESS_RECALCULATED, Epic10ReviewTargetType.REVIEW_FINDING, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic10ReviewAuditAction action,
            Epic10ReviewTargetType targetType,
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
