package com.homehealthcare.evv.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvvAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordClockInRecorded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.CLOCK_IN_RECORDED, Epic7EvvTargetType.EVV_CLOCK_EVENT, targetId, branchId, metadataJson);
    }

    public void recordClockOutRecorded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.CLOCK_OUT_RECORDED, Epic7EvvTargetType.EVV_CLOCK_EVENT, targetId, branchId, metadataJson);
    }

    public void recordGeofenceEvaluated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.GEOFENCE_EVALUATED, Epic7EvvTargetType.GEOFENCE_EVALUATION, targetId, branchId, metadataJson);
    }

    public void recordSignatureStatusRecorded(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.SIGNATURE_STATUS_RECORDED, Epic7EvvTargetType.SIGNATURE_VERIFICATION_LINK, targetId, branchId, metadataJson);
    }

    public void recordMissedVisitReported(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.MISSED_VISIT_REPORTED, Epic7EvvTargetType.MISSED_VISIT_RECORD, targetId, branchId, metadataJson);
    }

    public void recordExceptionLogged(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.EXCEPTION_LOGGED, Epic7EvvTargetType.VISIT_EXCEPTION_RECORD, targetId, branchId, metadataJson);
    }

    public void recordSupervisorNotified(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.SUPERVISOR_NOTIFIED, Epic7EvvTargetType.SUPERVISOR_NOTIFICATION_EVENT, targetId, branchId, metadataJson);
    }

    public void recordEscalationCreated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(actorMembership, Epic7EvvAuditAction.ESCALATION_CREATED, Epic7EvvTargetType.ESCALATION_REQUEST, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic7EvvAuditAction action,
            Epic7EvvTargetType targetType,
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
