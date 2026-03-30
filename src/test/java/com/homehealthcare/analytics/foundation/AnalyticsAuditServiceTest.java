package com.homehealthcare.analytics.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsAuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AnalyticsAuditService analyticsAuditService;

    @Test
    void analyticsAuditUsesStandardizedEpic15ActionAndTargetType() {
        AgencyMembership actorMembership = membership(AgencyRole.BRANCH_ADMIN);
        UUID targetId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        analyticsAuditService.recordDashboardSnapshotGenerated(
                actorMembership,
                targetId,
                branchId,
                "{\"metric\":\"TODAYS_VISITS\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent event = captor.getValue();

        assertThat(event.getActionType()).isEqualTo(Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType());
        assertThat(event.getTargetType()).isEqualTo(Epic15AnalyticsTargetType.DASHBOARD_METRIC_SNAPSHOT.name());
        assertThat(event.getTargetId()).isEqualTo(targetId);
        assertThat(event.getBranchId()).isEqualTo(branchId);
        assertThat(event.getActorEmail()).isEqualTo(actorMembership.getUser().getEmail());
    }

    @Test
    void analyticsFoundationContractsCoverExpectedMetricAndRefreshVocabulary() {
        assertThat(EnumSet.allOf(AnalyticsEntityCategory.class))
                .containsExactlyInAnyOrder(
                        AnalyticsEntityCategory.DASHBOARD_METRIC_DEFINITION,
                        AnalyticsEntityCategory.DASHBOARD_METRIC_SNAPSHOT,
                        AnalyticsEntityCategory.BRANCH_PERFORMANCE_SUMMARY,
                        AnalyticsEntityCategory.UTILIZATION_SUMMARY,
                        AnalyticsEntityCategory.BACKLOG_SUMMARY,
                        AnalyticsEntityCategory.READINESS_COMPLIANCE_SUMMARY,
                        AnalyticsEntityCategory.METRIC_TREND_SNAPSHOT,
                        AnalyticsEntityCategory.DASHBOARD_REFRESH_REQUEST);

        assertThat(EnumSet.allOf(AnalyticsDashboardMetricType.class))
                .containsExactlyInAnyOrder(
                        AnalyticsDashboardMetricType.TODAYS_VISITS,
                        AnalyticsDashboardMetricType.UNFILLED_VISITS,
                        AnalyticsDashboardMetricType.LATE_STARTS,
                        AnalyticsDashboardMetricType.MISSED_VISITS,
                        AnalyticsDashboardMetricType.DOCUMENTATION_AGING,
                        AnalyticsDashboardMetricType.QA_BACKLOG,
                        AnalyticsDashboardMetricType.CAREGIVER_UTILIZATION,
                        AnalyticsDashboardMetricType.BRANCH_PERFORMANCE,
                        AnalyticsDashboardMetricType.REVENUE_READINESS,
                        AnalyticsDashboardMetricType.COMPLIANCE_EXCEPTIONS);

        assertThat(EnumSet.allOf(AnalyticsRefreshMode.class))
                .containsExactlyInAnyOrder(
                        AnalyticsRefreshMode.NEAR_REAL_TIME,
                        AnalyticsRefreshMode.SCHEDULED,
                        AnalyticsRefreshMode.ON_DEMAND);
    }

    @Test
    void refreshContractCapturesMetricTypeModeAndFreshnessExpectation() {
        AnalyticsMetricRefreshContract contract = new AnalyticsMetricRefreshContract(
                AnalyticsDashboardMetricType.QA_BACKLOG,
                AnalyticsRefreshMode.SCHEDULED,
                30,
                "Refresh every 30 minutes from review source projections.");

        assertThat(contract.metricType()).isEqualTo(AnalyticsDashboardMetricType.QA_BACKLOG);
        assertThat(contract.refreshMode()).isEqualTo(AnalyticsRefreshMode.SCHEDULED);
        assertThat(contract.maxStalenessMinutes()).isEqualTo(30);
        assertThat(contract.refreshExpectation()).contains("30 minutes");
    }

    private AgencyMembership membership(AgencyRole role) {
        return AgencyMembership.grant(
                User.invite("Analytics", "Admin", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
    }
}
