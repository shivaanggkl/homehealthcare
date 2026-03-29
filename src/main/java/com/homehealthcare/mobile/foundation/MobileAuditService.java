package com.homehealthcare.mobile.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MobileAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordMobileSessionBootstrapped(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.MOBILE_SESSION_BOOTSTRAPPED, Epic6MobileTargetType.MOBILE_DEVICE_SESSION, targetId, branchId, metadataJson);
    }

    public void recordVisitExecutionStarted(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.VISIT_EXECUTION_STARTED, Epic6MobileTargetType.VISIT_EXECUTION_SESSION, targetId, branchId, metadataJson);
    }

    public void recordVisitExecutionEnded(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.VISIT_EXECUTION_ENDED, Epic6MobileTargetType.VISIT_EXECUTION_SESSION, targetId, branchId, metadataJson);
    }

    public void recordQuickNoteSaved(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.QUICK_NOTE_SAVED, Epic6MobileTargetType.QUICK_NOTE_ENTRY, targetId, branchId, metadataJson);
    }

    public void recordTaskChecklistSaved(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.TASK_CHECKLIST_SAVED, Epic6MobileTargetType.TASK_CHECKLIST_ENTRY, targetId, branchId, metadataJson);
    }

    public void recordIncidentFlagged(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.INCIDENT_FLAGGED, Epic6MobileTargetType.INCIDENT_REPORT, targetId, branchId, metadataJson);
    }

    public void recordOfflineSyncAccepted(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.OFFLINE_SYNC_ACCEPTED, Epic6MobileTargetType.OFFLINE_SYNC_ENVELOPE, targetId, branchId, metadataJson);
    }

    public void recordOfflineSyncRejected(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic6MobileAuditAction.OFFLINE_SYNC_REJECTED, Epic6MobileTargetType.OFFLINE_SYNC_ENVELOPE, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic6MobileAuditAction action,
            Epic6MobileTargetType targetType,
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
