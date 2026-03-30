package com.homehealthcare.careprogression.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CareProgressionAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordGoalTemplateSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.GOAL_TEMPLATE_SAVED, Epic13CareProgressionTargetType.GOAL_TEMPLATE, targetId, branchId, metadataJson);
    }

    public void recordPatientGoalCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.PATIENT_GOAL_CREATED, Epic13CareProgressionTargetType.PATIENT_GOAL, targetId, branchId, metadataJson);
    }

    public void recordPatientGoalUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.PATIENT_GOAL_UPDATED, Epic13CareProgressionTargetType.PATIENT_GOAL, targetId, branchId, metadataJson);
    }

    public void recordInterventionSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.INTERVENTION_SAVED, Epic13CareProgressionTargetType.GOAL_INTERVENTION, targetId, branchId, metadataJson);
    }

    public void recordProgressNoteAdded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.PROGRESS_NOTE_ADDED, Epic13CareProgressionTargetType.GOAL_PROGRESS_NOTE, targetId, branchId, metadataJson);
    }

    public void recordGoalStateChanged(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.GOAL_STATE_CHANGED, Epic13CareProgressionTargetType.PATIENT_GOAL, targetId, branchId, metadataJson);
    }

    public void recordTargetDateChanged(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.TARGET_DATE_CHANGED, Epic13CareProgressionTargetType.PATIENT_GOAL, targetId, branchId, metadataJson);
    }

    public void recordGoalVersionRecorded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.GOAL_VERSION_RECORDED, Epic13CareProgressionTargetType.GOAL_VERSION, targetId, branchId, metadataJson);
    }

    public void recordCarePlanSyncUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.CAREPLAN_SYNC_UPDATED, Epic13CareProgressionTargetType.CAREPLAN_SYNC_LINK, targetId, branchId, metadataJson);
    }

    public void recordProgressionEventPublished(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic13CareProgressionAuditAction.PROGRESSION_EVENT_PUBLISHED, Epic13CareProgressionTargetType.PROGRESSION_EVENT, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic13CareProgressionAuditAction action,
            Epic13CareProgressionTargetType targetType,
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
