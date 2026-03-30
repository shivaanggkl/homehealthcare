package com.homehealthcare.compliance.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ComplianceAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordChecklistDefinitionSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.CHECKLIST_DEFINITION_SAVED, Epic11ComplianceTargetType.CHECKLIST_DEFINITION, targetId, branchId, metadataJson);
    }

    public void recordChecklistResultRecalculated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.CHECKLIST_RESULT_RECALCULATED, Epic11ComplianceTargetType.CHECKLIST_RESULT, targetId, branchId, metadataJson);
    }

    public void recordDocumentationRequirementSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.DOCUMENTATION_REQUIREMENT_SAVED, Epic11ComplianceTargetType.DOCUMENTATION_REQUIREMENT, targetId, branchId, metadataJson);
    }

    public void recordAcknowledgmentRecorded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.ACKNOWLEDGMENT_RECORDED, Epic11ComplianceTargetType.ACKNOWLEDGMENT_RECORD, targetId, branchId, metadataJson);
    }

    public void recordAcknowledgmentRevoked(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.ACKNOWLEDGMENT_REVOKED, Epic11ComplianceTargetType.ACKNOWLEDGMENT_RECORD, targetId, branchId, metadataJson);
    }

    public void recordCertificationPeriodSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.CERTIFICATION_PERIOD_SAVED, Epic11ComplianceTargetType.CERTIFICATION_PERIOD, targetId, branchId, metadataJson);
    }

    public void recordRiskReminderSaved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.RISK_REMINDER_SAVED, Epic11ComplianceTargetType.RISK_REMINDER, targetId, branchId, metadataJson);
    }

    public void recordRiskReminderResolved(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.RISK_REMINDER_RESOLVED, Epic11ComplianceTargetType.RISK_REMINDER, targetId, branchId, metadataJson);
    }

    public void recordStatusProjectionRecalculated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED, Epic11ComplianceTargetType.STATUS_PROJECTION, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic11ComplianceAuditAction action,
            Epic11ComplianceTargetType targetType,
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
