package com.homehealthcare.revenuereadiness.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueReadinessAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordReadinessRecalculated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic14RevenueReadinessAuditAction.READINESS_RECALCULATED,
                Epic14RevenueReadinessTargetType.REVENUE_READINESS_PROJECTION,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordExceptionFlagUpdated(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic14RevenueReadinessAuditAction.EXCEPTION_FLAG_UPDATED,
                Epic14RevenueReadinessTargetType.REVENUE_EXCEPTION_FLAG,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordExportGenerated(
            AgencyMembership actorMembership,
            Epic14RevenueReadinessTargetType targetType,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        if (targetType != Epic14RevenueReadinessTargetType.PAYROLL_EXPORT_ROW
                && targetType != Epic14RevenueReadinessTargetType.INVOICE_EXPORT_ROW) {
            throw new IllegalArgumentException("Revenue export audits must target payroll or invoice export rows.");
        }
        record(
                actorMembership,
                Epic14RevenueReadinessAuditAction.EXPORT_GENERATED,
                targetType,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordAuthorizationUsageRefreshed(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic14RevenueReadinessAuditAction.AUTHORIZATION_USAGE_REFRESHED,
                Epic14RevenueReadinessTargetType.AUTHORIZATION_USAGE_SNAPSHOT,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordPayerServiceSummaryRefreshed(AgencyMembership actorMembership, UUID targetId, UUID branchId, String metadataJson) {
        record(
                actorMembership,
                Epic14RevenueReadinessAuditAction.PAYER_SERVICE_SUMMARY_REFRESHED,
                Epic14RevenueReadinessTargetType.PAYER_SERVICE_SUMMARY_PROJECTION,
                targetId,
                branchId,
                metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic14RevenueReadinessAuditAction action,
            Epic14RevenueReadinessTargetType targetType,
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
