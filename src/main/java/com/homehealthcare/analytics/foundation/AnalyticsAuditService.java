package com.homehealthcare.analytics.foundation;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsAuditService {

    private static final String ACTOR_TYPE = "AGENCY_MEMBERSHIP";

    private final AuditEventRepository auditEventRepository;

    public void recordDashboardSnapshotGenerated(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED,
                Epic15AnalyticsTargetType.DASHBOARD_METRIC_SNAPSHOT,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordMetricRefreshTriggered(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.METRIC_REFRESH_TRIGGERED,
                Epic15AnalyticsTargetType.DASHBOARD_REFRESH_REQUEST,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordBranchPerformanceRefreshed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.BRANCH_PERFORMANCE_REFRESHED,
                Epic15AnalyticsTargetType.BRANCH_PERFORMANCE_SUMMARY,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordUtilizationSummaryRefreshed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.UTILIZATION_SUMMARY_REFRESHED,
                Epic15AnalyticsTargetType.UTILIZATION_SUMMARY,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordQaBacklogSummaryRefreshed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.QA_BACKLOG_SUMMARY_REFRESHED,
                Epic15AnalyticsTargetType.BACKLOG_SUMMARY,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordRevenueReadinessSummaryRefreshed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.REVENUE_READINESS_SUMMARY_REFRESHED,
                Epic15AnalyticsTargetType.READINESS_COMPLIANCE_SUMMARY,
                targetId,
                branchId,
                metadataJson);
    }

    public void recordComplianceSummaryRefreshed(
            AgencyMembership actorMembership,
            UUID targetId,
            UUID branchId,
            String metadataJson) {
        record(actorMembership,
                Epic15AnalyticsAuditAction.COMPLIANCE_SUMMARY_REFRESHED,
                Epic15AnalyticsTargetType.READINESS_COMPLIANCE_SUMMARY,
                targetId,
                branchId,
                metadataJson);
    }

    private void record(
            AgencyMembership actorMembership,
            Epic15AnalyticsAuditAction action,
            Epic15AnalyticsTargetType targetType,
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
