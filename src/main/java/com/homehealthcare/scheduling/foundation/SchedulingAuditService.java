package com.homehealthcare.scheduling.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulingAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordVisitCreated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.VISIT_CREATED, Epic5SchedulingTargetType.VISIT_OCCURRENCE, targetId, branchId, metadataJson);
    }

    public void recordVisitUpdated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.VISIT_UPDATED, Epic5SchedulingTargetType.VISIT_OCCURRENCE, targetId, branchId, metadataJson);
    }

    public void recordRecurringRuleCreated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.RECURRING_RULE_CREATED, Epic5SchedulingTargetType.RECURRING_VISIT_RULE, targetId, branchId, metadataJson);
    }

    public void recordRecurringRuleUpdated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.RECURRING_RULE_UPDATED, Epic5SchedulingTargetType.RECURRING_VISIT_RULE, targetId, branchId, metadataJson);
    }

    public void recordCaregiverAssigned(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.CAREGIVER_ASSIGNED, Epic5SchedulingTargetType.CAREGIVER_ASSIGNMENT, targetId, branchId, metadataJson);
    }

    public void recordAssignmentRemoved(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.ASSIGNMENT_REMOVED, Epic5SchedulingTargetType.CAREGIVER_ASSIGNMENT, targetId, branchId, metadataJson);
    }

    public void recordOpenShiftCreated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.OPEN_SHIFT_CREATED, Epic5SchedulingTargetType.OPEN_SHIFT, targetId, branchId, metadataJson);
    }

    public void recordOpenShiftClosed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.OPEN_SHIFT_CLOSED, Epic5SchedulingTargetType.OPEN_SHIFT, targetId, branchId, metadataJson);
    }

    public void recordVisitRescheduled(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.VISIT_RESCHEDULED, Epic5SchedulingTargetType.RESCHEDULE_EVENT, targetId, branchId, metadataJson);
    }

    public void recordVisitCancelled(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.VISIT_CANCELLED, Epic5SchedulingTargetType.CANCELLATION_EVENT, targetId, branchId, metadataJson);
    }

    public void recordConflictFlagged(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.CONFLICT_FLAGGED, Epic5SchedulingTargetType.CONFLICT_EVALUATION, targetId, branchId, metadataJson);
    }

    public void recordTravelEvaluated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic5SchedulingAuditAction.TRAVEL_EVALUATED, Epic5SchedulingTargetType.TRAVEL_AWARENESS_EVALUATION, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic5SchedulingAuditAction action,
            Epic5SchedulingTargetType targetType,
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
