package com.homehealthcare.patientevent.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientEventAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordIncidentCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.INCIDENT_CREATED, Epic12PatientEventTargetType.INCIDENT_RECORD, targetId, branchId, metadataJson);
    }

    public void recordIncidentUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.INCIDENT_UPDATED, Epic12PatientEventTargetType.INCIDENT_RECORD, targetId, branchId, metadataJson);
    }

    public void recordInfectionCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.INFECTION_CREATED, Epic12PatientEventTargetType.INFECTION_RECORD, targetId, branchId, metadataJson);
    }

    public void recordInfectionUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.INFECTION_UPDATED, Epic12PatientEventTargetType.INFECTION_RECORD, targetId, branchId, metadataJson);
    }

    public void recordWoundCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.WOUND_CREATED, Epic12PatientEventTargetType.WOUND_RECORD, targetId, branchId, metadataJson);
    }

    public void recordWoundUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.WOUND_UPDATED, Epic12PatientEventTargetType.WOUND_RECORD, targetId, branchId, metadataJson);
    }

    public void recordWoundHistoryAdded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.WOUND_HISTORY_ADDED, Epic12PatientEventTargetType.WOUND_HISTORY_ENTRY, targetId, branchId, metadataJson);
    }

    public void recordEvidenceLinked(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.EVIDENCE_LINKED, Epic12PatientEventTargetType.EVIDENCE_LINK, targetId, branchId, metadataJson);
    }

    public void recordEvidenceUnlinked(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.EVIDENCE_UNLINKED, Epic12PatientEventTargetType.EVIDENCE_LINK, targetId, branchId, metadataJson);
    }

    public void recordFollowUpAssigned(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.FOLLOW_UP_ASSIGNED, Epic12PatientEventTargetType.FOLLOW_UP_ASSIGNMENT, targetId, branchId, metadataJson);
    }

    public void recordFollowUpUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.FOLLOW_UP_UPDATED, Epic12PatientEventTargetType.FOLLOW_UP_ASSIGNMENT, targetId, branchId, metadataJson);
    }

    public void recordEscalationCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.ESCALATION_CREATED, Epic12PatientEventTargetType.ESCALATION_RECORD, targetId, branchId, metadataJson);
    }

    public void recordEscalationCleared(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.ESCALATION_CLEARED, Epic12PatientEventTargetType.ESCALATION_RECORD, targetId, branchId, metadataJson);
    }

    public void recordRecordResolved(
            AgencyMembership actorMembership,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic12PatientEventAuditAction.RECORD_RESOLVED, targetType, targetId, branchId, metadataJson);
    }

    public void recordLongitudinalHistoryProjected(AgencyMembership actorMembership, UUID patientId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic12PatientEventAuditAction.LONGITUDINAL_HISTORY_PROJECTED,
                Epic12PatientEventTargetType.LONGITUDINAL_HISTORY_ENTRY,
                patientId,
                branchId,
                metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic12PatientEventAuditAction action,
            Epic12PatientEventTargetType targetType,
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
