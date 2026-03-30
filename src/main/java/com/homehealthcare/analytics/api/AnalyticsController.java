package com.homehealthcare.analytics.api;

import com.homehealthcare.analytics.application.AnalyticsService;
import com.homehealthcare.analytics.application.AnalyticsService.BacklogDrilldownItem;
import com.homehealthcare.analytics.application.AnalyticsService.ComplianceDrilldownItem;
import com.homehealthcare.analytics.application.AnalyticsService.DashboardAnalyticsView;
import com.homehealthcare.analytics.application.AnalyticsService.DashboardSummaryView;
import com.homehealthcare.analytics.application.AnalyticsService.OperationalDrilldownItem;
import com.homehealthcare.analytics.application.AnalyticsService.ReadinessDrilldownItem;
import com.homehealthcare.analytics.application.AnalyticsService.RefreshDashboardSnapshotCommand;
import com.homehealthcare.analytics.domain.BacklogSummary;
import com.homehealthcare.analytics.domain.BranchPerformanceSummary;
import com.homehealthcare.analytics.domain.DashboardMetricSnapshot;
import com.homehealthcare.analytics.domain.DashboardSnapshot;
import com.homehealthcare.analytics.domain.MetricTrendSnapshot;
import com.homehealthcare.analytics.domain.ReadinessComplianceSummary;
import com.homehealthcare.analytics.domain.UtilizationSummary;
import com.homehealthcare.analytics.foundation.AnalyticsDashboardMetricType;
import com.homehealthcare.compliance.foundation.ComplianceReadinessStatus;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController {

    private final ConfigurationActorResolver actorResolver;
    private final AnalyticsService analyticsService;

    AnalyticsController(ConfigurationActorResolver actorResolver, AnalyticsService analyticsService) {
        this.actorResolver = actorResolver;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    DashboardSummaryResponse getDashboardSummary(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return toDashboardSummaryResponse(analyticsService.getDashboardSummary(
                actorResolver.requireActorMembership(),
                snapshotDate,
                branchId));
    }

    @GetMapping("/metrics")
    PagedResponse<MetricSnapshotResponse> listMetricSnapshots(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "metricType", required = false) AnalyticsDashboardMetricType metricType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listMetricSnapshots(
                                actorResolver.requireActorMembership(),
                                snapshotDate,
                                branchId,
                                metricType).stream()
                        .map(AnalyticsController::toMetricSnapshotResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/drilldowns/operational")
    PagedResponse<OperationalDrilldownResponse> listOperationalDrilldown(
            @RequestParam("metricType") AnalyticsDashboardMetricType metricType,
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listOperationalDrilldown(
                                actorResolver.requireActorMembership(),
                                metricType,
                                snapshotDate,
                                branchId,
                                status).stream()
                        .map(AnalyticsController::toOperationalDrilldownResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/drilldowns/backlog")
    PagedResponse<BacklogDrilldownResponse> listBacklogDrilldown(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listBacklogDrilldown(
                                actorResolver.requireActorMembership(),
                                snapshotDate,
                                branchId,
                                status).stream()
                        .map(AnalyticsController::toBacklogDrilldownResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/drilldowns/revenue")
    PagedResponse<ReadinessDrilldownResponse> listRevenueDrilldown(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "readinessStatus", required = false) RevenueReadinessStatus readinessStatus,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listRevenueReadinessDrilldown(
                                actorResolver.requireActorMembership(),
                                snapshotDate,
                                branchId,
                                readinessStatus).stream()
                        .map(AnalyticsController::toReadinessDrilldownResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/drilldowns/compliance")
    PagedResponse<ComplianceDrilldownResponse> listComplianceDrilldown(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "readinessStatus", required = false) ComplianceReadinessStatus readinessStatus,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listComplianceDrilldown(
                                actorResolver.requireActorMembership(),
                                branchId,
                                readinessStatus).stream()
                        .map(AnalyticsController::toComplianceDrilldownResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/branch-performance")
    PagedResponse<BranchPerformanceSummaryResponse> listBranchPerformance(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listBranchPerformanceSummaries(
                                actorResolver.requireActorMembership(),
                                snapshotDate,
                                branchId).stream()
                        .map(AnalyticsController::toBranchPerformanceSummaryResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/caregiver-utilization")
    PagedResponse<UtilizationSummaryResponse> listCaregiverUtilization(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "caregiverProfileId", required = false) UUID caregiverProfileId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listUtilizationSummaries(
                                actorResolver.requireActorMembership(),
                                snapshotDate,
                                branchId).stream()
                        .filter(summary -> caregiverProfileId == null || caregiverProfileId.equals(summary.getCaregiverProfileId()))
                        .map(AnalyticsController::toUtilizationSummaryResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/trends")
    PagedResponse<MetricTrendSnapshotResponse> listTrendSnapshots(
            @RequestParam("snapshotDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "metricType", required = false) AnalyticsDashboardMetricType metricType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listMetricTrendSnapshots(
                                actorResolver.requireActorMembership(),
                                snapshotDate,
                                branchId,
                                metricType).stream()
                        .map(AnalyticsController::toMetricTrendSnapshotResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/history/dashboard-snapshots")
    PagedResponse<DashboardSnapshotResponse> listDashboardSnapshotHistory(
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listDashboardSnapshotHistory(
                                actorResolver.requireActorMembership(),
                                fromDate,
                                toDate,
                                branchId).stream()
                        .map(AnalyticsController::toDashboardSnapshotResponse)
                        .toList(),
                page,
                size);
    }

    @GetMapping("/history/metric-refreshes")
    PagedResponse<AuditEventResponse> listMetricRefreshHistory(
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        return page(
                analyticsService.listMetricRefreshHistory(actorResolver.requireActorMembership(), branchId).stream()
                        .map(AnalyticsController::toAuditEventResponse)
                        .toList(),
                page,
                size);
    }

    @PostMapping("/dashboard/refresh")
    DashboardAnalyticsResponse refreshDashboard(@Valid @RequestBody RefreshDashboardRequest request) {
        return toDashboardAnalyticsResponse(analyticsService.refreshDashboardSnapshot(
                actorResolver.requireActorMembership(),
                new RefreshDashboardSnapshotCommand(
                        request.snapshotDate(),
                        request.branchId(),
                        request.evaluatedAt(),
                        request.lateStartThresholdMinutes(),
                        request.includeTrendSnapshots())));
    }

    private static DashboardAnalyticsResponse toDashboardAnalyticsResponse(DashboardAnalyticsView view) {
        return new DashboardAnalyticsResponse(
                view.dashboardSnapshot() == null ? null : toDashboardSnapshotResponse(view.dashboardSnapshot()),
                view.metricSnapshots().stream().map(AnalyticsController::toMetricSnapshotResponse).toList(),
                view.branchPerformanceSummaries().stream().map(AnalyticsController::toBranchPerformanceSummaryResponse).toList(),
                view.utilizationSummaries().stream().map(AnalyticsController::toUtilizationSummaryResponse).toList(),
                view.backlogSummaries().stream().map(AnalyticsController::toBacklogSummaryResponse).toList(),
                view.readinessComplianceSummaries().stream().map(AnalyticsController::toReadinessComplianceSummaryResponse).toList(),
                view.trendSnapshots().stream().map(AnalyticsController::toMetricTrendSnapshotResponse).toList());
    }

    private static DashboardSummaryResponse toDashboardSummaryResponse(DashboardSummaryView view) {
        return new DashboardSummaryResponse(
                view.dashboardSnapshot() == null ? null : toDashboardSnapshotResponse(view.dashboardSnapshot()),
                view.branchPerformanceSummaries().stream().map(AnalyticsController::toBranchPerformanceSummaryResponse).toList(),
                view.backlogSummaries().stream().map(AnalyticsController::toBacklogSummaryResponse).toList(),
                view.readinessComplianceSummaries().stream().map(AnalyticsController::toReadinessComplianceSummaryResponse).toList());
    }

    private static DashboardSnapshotResponse toDashboardSnapshotResponse(DashboardSnapshot snapshot) {
        return new DashboardSnapshotResponse(
                snapshot.getId(),
                snapshot.getBranchId(),
                snapshot.getSnapshotDate(),
                snapshot.getTodaysVisitCount(),
                snapshot.getUnfilledVisitCount(),
                snapshot.getLateStartCount(),
                snapshot.getMissedVisitCount(),
                snapshot.getDocumentationAgingCount(),
                snapshot.getQaBacklogCount(),
                snapshot.getCaregiverUtilizationCount(),
                snapshot.getRevenueBlockedCount(),
                snapshot.getComplianceExceptionCount(),
                snapshot.getGeneratedAt());
    }

    private static MetricSnapshotResponse toMetricSnapshotResponse(DashboardMetricSnapshot snapshot) {
        return new MetricSnapshotResponse(
                snapshot.getId(),
                snapshot.getMetricDefinition().getMetricType().name(),
                snapshot.getMetricDefinition().getMetricScope().name(),
                snapshot.getBranchId(),
                snapshot.getSnapshotDate(),
                snapshot.getPrimaryValue(),
                snapshot.getSecondaryValue(),
                snapshot.getStatusCode(),
                snapshot.getDrilldownReferenceJson(),
                snapshot.getCapturedAt());
    }

    private static BranchPerformanceSummaryResponse toBranchPerformanceSummaryResponse(BranchPerformanceSummary summary) {
        return new BranchPerformanceSummaryResponse(
                summary.getId(),
                summary.getBranchId(),
                summary.getSnapshotDate(),
                summary.getTodaysVisitCount(),
                summary.getUnfilledVisitCount(),
                summary.getLateStartCount(),
                summary.getMissedVisitCount(),
                summary.getDocumentationAgingCount(),
                summary.getQaBacklogCount(),
                summary.getPosture().name(),
                summary.getEvaluatedAt());
    }

    private static UtilizationSummaryResponse toUtilizationSummaryResponse(UtilizationSummary summary) {
        return new UtilizationSummaryResponse(
                summary.getId(),
                summary.getBranchId(),
                summary.getCaregiverProfileId(),
                summary.getSnapshotDate(),
                summary.getAssignedVisitCount(),
                summary.getCompletedVisitCount(),
                summary.getScheduledMinutes(),
                summary.getPosture().name(),
                summary.getEvaluatedAt());
    }

    private static BacklogSummaryResponse toBacklogSummaryResponse(BacklogSummary summary) {
        return new BacklogSummaryResponse(
                summary.getId(),
                summary.getBranchId(),
                summary.getSnapshotDate(),
                summary.getDocumentationAgingCount(),
                summary.getAgingBucketJson(),
                summary.getPendingReviewCount(),
                summary.getReturnedForFixCount(),
                summary.getOverdueReviewCount(),
                summary.getPosture().name(),
                summary.getEvaluatedAt());
    }

    private static ReadinessComplianceSummaryResponse toReadinessComplianceSummaryResponse(ReadinessComplianceSummary summary) {
        return new ReadinessComplianceSummaryResponse(
                summary.getId(),
                summary.getBranchId(),
                summary.getSnapshotDate(),
                summary.getRevenueReadyCount(),
                summary.getRevenueWarningCount(),
                summary.getRevenueBlockedCount(),
                summary.getComplianceReadyCount(),
                summary.getComplianceWarningCount(),
                summary.getComplianceExceptionCount(),
                summary.getEvaluatedAt());
    }

    private static MetricTrendSnapshotResponse toMetricTrendSnapshotResponse(MetricTrendSnapshot snapshot) {
        return new MetricTrendSnapshotResponse(
                snapshot.getId(),
                snapshot.getMetricType().name(),
                snapshot.getBranchId(),
                snapshot.getSnapshotDate(),
                snapshot.getCurrentValue(),
                snapshot.getPreviousValue(),
                snapshot.getDeltaValue(),
                snapshot.getDirection().name(),
                snapshot.getCalculatedAt());
    }

    private static OperationalDrilldownResponse toOperationalDrilldownResponse(OperationalDrilldownItem item) {
        return new OperationalDrilldownResponse(
                item.visitOccurrenceId(),
                item.patientId(),
                item.branchId(),
                item.caregiverProfileId(),
                item.caregiverDisplayName(),
                item.plannedStartAt(),
                item.plannedEndAt(),
                item.visitStatus(),
                item.detailStatus());
    }

    private static BacklogDrilldownResponse toBacklogDrilldownResponse(BacklogDrilldownItem item) {
        return new BacklogDrilldownResponse(
                item.reviewWorkItemId(),
                item.sourceRecordId(),
                item.patientId(),
                item.branchId(),
                item.status(),
                item.priority(),
                item.enteredQueueAt(),
                item.dueAt(),
                item.overdue());
    }

    private static ReadinessDrilldownResponse toReadinessDrilldownResponse(ReadinessDrilldownItem item) {
        return new ReadinessDrilldownResponse(
                item.visitOccurrenceId(),
                item.patientId(),
                item.branchId(),
                item.readinessStatus(),
                item.exceptionCount(),
                item.warningCount(),
                item.evaluatedAt());
    }

    private static ComplianceDrilldownResponse toComplianceDrilldownResponse(ComplianceDrilldownItem item) {
        return new ComplianceDrilldownResponse(
                item.patientId(),
                item.branchId(),
                item.serviceLineId(),
                item.readinessStatus(),
                item.certificationPeriodStatus(),
                item.activeRiskReminderCount(),
                item.gapCount(),
                item.evaluatedAt());
    }

    private static AuditEventResponse toAuditEventResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActionType(),
                event.getOutcome().name(),
                event.getTargetType(),
                event.getTargetId(),
                event.getBranchId(),
                event.getOccurredAt(),
                event.getMetadataJson());
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), (items.size() + size - 1) / size);
    }

    record RefreshDashboardRequest(
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            java.time.OffsetDateTime evaluatedAt,
            int lateStartThresholdMinutes,
            boolean includeTrendSnapshots) {
    }

    record DashboardAnalyticsResponse(
            DashboardSnapshotResponse dashboardSnapshot,
            List<MetricSnapshotResponse> metricSnapshots,
            List<BranchPerformanceSummaryResponse> branchPerformanceSummaries,
            List<UtilizationSummaryResponse> utilizationSummaries,
            List<BacklogSummaryResponse> backlogSummaries,
            List<ReadinessComplianceSummaryResponse> readinessComplianceSummaries,
            List<MetricTrendSnapshotResponse> trendSnapshots) {
    }

    record DashboardSummaryResponse(
            DashboardSnapshotResponse dashboardSnapshot,
            List<BranchPerformanceSummaryResponse> branchPerformanceSummaries,
            List<BacklogSummaryResponse> backlogSummaries,
            List<ReadinessComplianceSummaryResponse> readinessComplianceSummaries) {
    }

    record DashboardSnapshotResponse(
            UUID id,
            UUID branchId,
            LocalDate snapshotDate,
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            int caregiverUtilizationCount,
            int revenueBlockedCount,
            int complianceExceptionCount,
            java.time.OffsetDateTime generatedAt) {
    }

    record MetricSnapshotResponse(
            UUID id,
            String metricType,
            String metricScope,
            UUID branchId,
            LocalDate snapshotDate,
            int primaryValue,
            int secondaryValue,
            String statusCode,
            String drilldownReference,
            java.time.OffsetDateTime capturedAt) {
    }

    record BranchPerformanceSummaryResponse(
            UUID id,
            UUID branchId,
            LocalDate snapshotDate,
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            String performancePosture,
            java.time.OffsetDateTime evaluatedAt) {
    }

    record UtilizationSummaryResponse(
            UUID id,
            UUID branchId,
            UUID caregiverProfileId,
            LocalDate snapshotDate,
            int assignedVisitCount,
            int completedVisitCount,
            int scheduledMinutes,
            String utilizationPosture,
            java.time.OffsetDateTime evaluatedAt) {
    }

    record BacklogSummaryResponse(
            UUID id,
            UUID branchId,
            LocalDate snapshotDate,
            int documentationAgingCount,
            String agingBucketsJson,
            int pendingReviewCount,
            int returnedForFixCount,
            int overdueReviewCount,
            String backlogPosture,
            java.time.OffsetDateTime evaluatedAt) {
    }

    record ReadinessComplianceSummaryResponse(
            UUID id,
            UUID branchId,
            LocalDate snapshotDate,
            int revenueReadyCount,
            int revenueWarningCount,
            int revenueBlockedCount,
            int complianceReadyCount,
            int complianceWarningCount,
            int complianceExceptionCount,
            java.time.OffsetDateTime evaluatedAt) {
    }

    record MetricTrendSnapshotResponse(
            UUID id,
            String metricType,
            UUID branchId,
            LocalDate snapshotDate,
            int currentValue,
            int previousValue,
            int deltaValue,
            String direction,
            java.time.OffsetDateTime calculatedAt) {
    }

    record OperationalDrilldownResponse(
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            UUID caregiverProfileId,
            String caregiverDisplayName,
            java.time.OffsetDateTime plannedStartAt,
            java.time.OffsetDateTime plannedEndAt,
            String visitStatus,
            String detailStatus) {
    }

    record BacklogDrilldownResponse(
            UUID reviewWorkItemId,
            UUID sourceRecordId,
            UUID patientId,
            UUID branchId,
            String status,
            String priority,
            java.time.OffsetDateTime enteredQueueAt,
            java.time.OffsetDateTime dueAt,
            boolean overdue) {
    }

    record ReadinessDrilldownResponse(
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            String readinessStatus,
            int exceptionCount,
            int warningCount,
            java.time.OffsetDateTime evaluatedAt) {
    }

    record ComplianceDrilldownResponse(
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            String readinessStatus,
            String certificationPeriodStatus,
            int activeRiskReminderCount,
            int gapCount,
            java.time.OffsetDateTime evaluatedAt) {
    }

    record AuditEventResponse(
            UUID id,
            String actionType,
            String outcome,
            String targetType,
            UUID targetId,
            UUID branchId,
            java.time.Instant occurredAt,
            String metadataJson) {
    }
}
