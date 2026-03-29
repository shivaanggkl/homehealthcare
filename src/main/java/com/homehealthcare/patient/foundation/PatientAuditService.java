package com.homehealthcare.patient.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordCreated(
            AgencyMembership actorMembership,
            Epic3PatientTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.CREATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordUpdated(
            AgencyMembership actorMembership,
            Epic3PatientTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.UPDATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordDeactivated(
            AgencyMembership actorMembership,
            Epic3PatientTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.DEACTIVATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordArchived(
            AgencyMembership actorMembership,
            Epic3PatientTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.ARCHIVED, targetType, targetId, branchId, metadataJson);
    }

    public void recordDuplicateFlagged(
            AgencyMembership actorMembership,
            Epic3PatientTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.DUPLICATE_FLAGGED, targetType, targetId, branchId, metadataJson);
    }

    public void recordAttachmentUploaded(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.ATTACHMENT_UPLOADED, Epic3PatientTargetType.PATIENT_ATTACHMENT, targetId, branchId, metadataJson);
    }

    public void recordAttachmentDownloaded(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic3PatientAuditAction.ATTACHMENT_DOWNLOADED, Epic3PatientTargetType.PATIENT_ATTACHMENT, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic3PatientAuditAction action,
            Epic3PatientTargetType targetType,
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
