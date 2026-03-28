package com.homehealthcare.configuration.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordCreated(
            AgencyMembership actorMembership,
            Epic2ConfigurationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic2ConfigurationAuditAction.CREATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordUpdated(
            AgencyMembership actorMembership,
            Epic2ConfigurationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic2ConfigurationAuditAction.UPDATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordDeactivated(
            AgencyMembership actorMembership,
            Epic2ConfigurationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic2ConfigurationAuditAction.DEACTIVATED, targetType, targetId, branchId, metadataJson);
    }

    public void recordPublished(
            AgencyMembership actorMembership,
            Epic2ConfigurationTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership, Epic2ConfigurationAuditAction.PUBLISHED, targetType, targetId, branchId, metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic2ConfigurationAuditAction action,
            Epic2ConfigurationTargetType targetType,
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
