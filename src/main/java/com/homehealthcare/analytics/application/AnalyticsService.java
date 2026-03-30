package com.homehealthcare.analytics.application;

import com.homehealthcare.analytics.domain.BacklogSummary;
import com.homehealthcare.analytics.domain.BacklogSummaryRepository;
import com.homehealthcare.analytics.domain.BranchPerformanceSummary;
import com.homehealthcare.analytics.domain.BranchPerformanceSummaryRepository;
import com.homehealthcare.analytics.domain.DashboardMetricDefinition;
import com.homehealthcare.analytics.domain.DashboardMetricDefinitionRepository;
import com.homehealthcare.analytics.domain.DashboardMetricSnapshot;
import com.homehealthcare.analytics.domain.DashboardMetricSnapshotRepository;
import com.homehealthcare.analytics.domain.DashboardSnapshot;
import com.homehealthcare.analytics.domain.DashboardSnapshotRepository;
import com.homehealthcare.analytics.domain.MetricTrendSnapshot;
import com.homehealthcare.analytics.domain.MetricTrendSnapshotRepository;
import com.homehealthcare.analytics.domain.ReadinessComplianceSummary;
import com.homehealthcare.analytics.domain.ReadinessComplianceSummaryRepository;
import com.homehealthcare.analytics.domain.UtilizationSummary;
import com.homehealthcare.analytics.domain.UtilizationSummaryRepository;
import com.homehealthcare.analytics.foundation.AnalyticsBacklogPosture;
import com.homehealthcare.analytics.foundation.AnalyticsBranchPerformancePosture;
import com.homehealthcare.analytics.foundation.AnalyticsDashboardMetricType;
import com.homehealthcare.analytics.foundation.AnalyticsMetricScope;
import com.homehealthcare.analytics.foundation.AnalyticsRefreshMode;
import com.homehealthcare.analytics.foundation.AnalyticsTrendDirection;
import com.homehealthcare.analytics.foundation.AnalyticsUtilizationPosture;
import com.homehealthcare.analytics.foundation.AnalyticsAuditService;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.compliance.domain.ComplianceStatusProjection;
import com.homehealthcare.compliance.domain.ComplianceStatusProjectionRepository;
import com.homehealthcare.compliance.foundation.ComplianceReadinessStatus;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.domain.VisitDocumentationRecordRepository;
import com.homehealthcare.documentation.foundation.DocumentationRecordStatus;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.EvvVerificationSessionRepository;
import com.homehealthcare.evv.domain.MissedVisitRecord;
import com.homehealthcare.evv.domain.MissedVisitRecordRepository;
import com.homehealthcare.evv.domain.MissedVisitStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSessionRepository;
import com.homehealthcare.mobile.foundation.MobileExecutionSessionStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjection;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjectionRepository;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import com.homehealthcare.review.domain.ReviewWorkItem;
import com.homehealthcare.review.domain.ReviewWorkItemRepository;
import com.homehealthcare.review.foundation.ReviewLifecycleStatus;
import com.homehealthcare.review.foundation.ReviewSourceType;
import com.homehealthcare.scheduling.foundation.SchedulingVisitStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Set<MissedVisitStatus> ACTIVE_MISSED_VISIT_STATUSES = EnumSet.of(
            MissedVisitStatus.REPORTED,
            MissedVisitStatus.NOTIFIED,
            MissedVisitStatus.ESCALATED);
    private static final Set<DocumentationRecordStatus> COMPLETED_DOCUMENTATION_STATUSES = Set.of(
            DocumentationRecordStatus.SUBMITTED,
            DocumentationRecordStatus.AMENDED,
            DocumentationRecordStatus.LOCKED);
    private static final Set<ReviewLifecycleStatus> QA_BACKLOG_STATUSES = EnumSet.of(
            ReviewLifecycleStatus.PENDING_REVIEW,
            ReviewLifecycleStatus.ASSIGNED,
            ReviewLifecycleStatus.IN_REVIEW,
            ReviewLifecycleStatus.RETURNED_FOR_FIX,
            ReviewLifecycleStatus.RESUBMITTED,
            ReviewLifecycleStatus.SIGNOFF_REQUESTED);

    private final DashboardMetricDefinitionRepository dashboardMetricDefinitionRepository;
    private final DashboardMetricSnapshotRepository dashboardMetricSnapshotRepository;
    private final BranchPerformanceSummaryRepository branchPerformanceSummaryRepository;
    private final UtilizationSummaryRepository utilizationSummaryRepository;
    private final BacklogSummaryRepository backlogSummaryRepository;
    private final ReadinessComplianceSummaryRepository readinessComplianceSummaryRepository;
    private final MetricTrendSnapshotRepository metricTrendSnapshotRepository;
    private final DashboardSnapshotRepository dashboardSnapshotRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final EvvVerificationSessionRepository evvVerificationSessionRepository;
    private final MissedVisitRecordRepository missedVisitRecordRepository;
    private final VisitDocumentationRecordRepository visitDocumentationRecordRepository;
    private final ReviewWorkItemRepository reviewWorkItemRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final MobileVisitExecutionSessionRepository mobileVisitExecutionSessionRepository;
    private final RevenueReadinessProjectionRepository revenueReadinessProjectionRepository;
    private final ComplianceStatusProjectionRepository complianceStatusProjectionRepository;
    private final BranchRepository branchRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final AnalyticsAuditService analyticsAuditService;
    @SuppressWarnings("unused")
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public DashboardAnalyticsView refreshDashboardSnapshot(
            @NotNull AgencyMembership actorMembership,
            @Valid RefreshDashboardSnapshotCommand command) {
        requirePermission(actorMembership, AgencyPermission.REFRESH_DASHBOARD_METRICS, "refresh dashboard metrics");
        List<Branch> branches = resolveBranchesInScope(actorMembership, AgencyPermission.REFRESH_DASHBOARD_METRICS, command.branchId(), "refresh dashboard metrics");
        LocalDate snapshotDate = command.snapshotDate();
        OffsetDateTime evaluatedAt = evaluatedAt(command.evaluatedAt());

        analyticsAuditService.recordMetricRefreshTriggered(
                actorMembership,
                UUID.nameUUIDFromBytes(("analytics-refresh|" + actorMembership.getAgencyId() + "|" + snapshotDate + "|" + Objects.toString(command.branchId(), "all")).getBytes(StandardCharsets.UTF_8)),
                command.branchId(),
                metricRefreshMetadata(snapshotDate, command.branchId(), command.lateStartThresholdMinutes()));

        Map<UUID, List<VisitOccurrence>> visitsByBranch = visitsByBranch(actorMembership.getAgencyId(), branches, snapshotDate);
        List<DashboardMetricSnapshot> metricSnapshots = new ArrayList<>();
        List<BranchPerformanceSummary> branchPerformanceSummaries = new ArrayList<>();
        List<UtilizationSummary> utilizationSummaries = new ArrayList<>();
        List<BacklogSummary> backlogSummaries = new ArrayList<>();
        List<ReadinessComplianceSummary> readinessSummaries = new ArrayList<>();
        List<MetricTrendSnapshot> trendSnapshots = new ArrayList<>();

        MetricAccumulator aggregate = new MetricAccumulator();
        for (Branch branch : branches) {
            BranchAnalytics branchAnalytics = buildBranchAnalytics(actorMembership.getAgencyId(), branch, visitsByBranch.getOrDefault(branch.getId(), List.of()), snapshotDate, evaluatedAt, command.lateStartThresholdMinutes());
            aggregate.accumulate(branchAnalytics);

            metricSnapshots.addAll(saveBranchMetricSnapshots(branch, snapshotDate, evaluatedAt, branchAnalytics));
            branchPerformanceSummaries.add(saveBranchPerformanceSummary(actorMembership, branch, snapshotDate, evaluatedAt, branchAnalytics));
            utilizationSummaries.addAll(saveUtilizationSummaries(actorMembership, branch, snapshotDate, evaluatedAt, branchAnalytics.utilizationRows()));
            backlogSummaries.add(saveBacklogSummary(actorMembership, branch, snapshotDate, evaluatedAt, branchAnalytics));
            readinessSummaries.add(saveReadinessComplianceSummary(actorMembership, branch, snapshotDate, evaluatedAt, branchAnalytics));
        }

        DashboardSnapshot dashboardSnapshot = saveDashboardSnapshot(actorMembership, command.branchId() == null ? null : branches.get(0), snapshotDate, evaluatedAt, aggregate);
        if (command.includeTrendSnapshots()) {
            trendSnapshots.addAll(saveTrendSnapshots(actorMembership, command.branchId() == null ? null : branches.get(0), snapshotDate, evaluatedAt, metricSnapshots));
        }

        analyticsAuditService.recordDashboardSnapshotGenerated(
                actorMembership,
                dashboardSnapshot.getId(),
                dashboardSnapshot.getBranchId(),
                snapshotMetadata(snapshotDate, dashboardSnapshot.getBranchId(), dashboardSnapshot));

        return new DashboardAnalyticsView(
                dashboardSnapshot,
                metricSnapshots,
                branchPerformanceSummaries,
                utilizationSummaries,
                backlogSummaries,
                readinessSummaries,
                trendSnapshots);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryView getDashboardSummary(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, "view analytics dashboard summary");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, branchId, "view analytics dashboard summary");
        }
        DashboardSnapshot snapshot = dashboardSnapshotRepository.findAllByAgency_IdAndSnapshotDateOrderByGeneratedAtDesc(
                        actorMembership.getAgencyId(),
                        snapshotDate).stream()
                .filter(candidate -> branchId == null || Objects.equals(branchId, candidate.getBranchId()))
                .filter(candidate -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, candidate.getBranchId()))
                .findFirst()
                .orElse(null);
        List<BranchPerformanceSummary> branchPerformance = listBranchPerformanceSummaries(actorMembership, snapshotDate, branchId);
        List<BacklogSummary> backlogSummaries = listBacklogSummaries(actorMembership, snapshotDate, branchId);
        List<ReadinessComplianceSummary> readinessSummaries = listReadinessComplianceSummaries(actorMembership, snapshotDate, branchId);
        return new DashboardSummaryView(snapshot, branchPerformance, backlogSummaries, readinessSummaries);
    }

    @Transactional(readOnly = true)
    public List<DashboardMetricSnapshot> listMetricSnapshots(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            AnalyticsDashboardMetricType metricType) {
        requirePermission(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, "view dashboard metric snapshots");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, branchId, "view dashboard metric snapshots");
        }
        return dashboardMetricSnapshotRepository.findAllByAgency_IdAndSnapshotDateOrderByCapturedAtDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(snapshot -> metricType == null || snapshot.getMetricDefinition().getMetricType() == metricType)
                .filter(snapshot -> branchId == null || Objects.equals(branchId, snapshot.getBranchId()))
                .filter(snapshot -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, snapshot.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchPerformanceSummary> listBranchPerformanceSummaries(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_BRANCH_PERFORMANCE_METRICS, "view branch performance metrics");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_BRANCH_PERFORMANCE_METRICS, branchId, "view branch performance metrics");
        }
        return branchPerformanceSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(summary -> branchId == null || Objects.equals(branchId, summary.getBranchId()))
                .filter(summary -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_BRANCH_PERFORMANCE_METRICS, summary.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UtilizationSummary> listUtilizationSummaries(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_CAREGIVER_UTILIZATION_METRICS, "view caregiver utilization metrics");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_CAREGIVER_UTILIZATION_METRICS, branchId, "view caregiver utilization metrics");
        }
        return utilizationSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByScheduledMinutesDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(summary -> branchId == null || Objects.equals(branchId, summary.getBranchId()))
                .filter(summary -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_CAREGIVER_UTILIZATION_METRICS, summary.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BacklogSummary> listBacklogSummaries(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_QA_BACKLOG_METRICS, "view qa backlog metrics");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_QA_BACKLOG_METRICS, branchId, "view qa backlog metrics");
        }
        return backlogSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(summary -> branchId == null || Objects.equals(branchId, summary.getBranchId()))
                .filter(summary -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_QA_BACKLOG_METRICS, summary.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadinessComplianceSummary> listReadinessComplianceSummaries(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_METRICS, "view readiness and compliance metrics");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_METRICS, branchId, "view readiness and compliance metrics");
        }
        return readinessComplianceSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(summary -> branchId == null || Objects.equals(branchId, summary.getBranchId()))
                .filter(summary -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_METRICS, summary.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationalDrilldownItem> listOperationalDrilldown(
            @NotNull AgencyMembership actorMembership,
            @NotNull AnalyticsDashboardMetricType metricType,
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            String status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, "view analytics operational drilldown");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, branchId, "view analytics operational drilldown");
        }
        String normalizedStatus = normalizeOptional(status);
        Map<UUID, CaregiverVisitAssignment> assignments = latestActiveAssignments(
                actorMembership.getAgencyId(),
                visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                        .map(VisitOccurrence::getId)
                        .toList());
        Map<UUID, EvvVerificationSession> verifications = latestVerificationSessions(
                visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                        .map(VisitOccurrence::getId)
                        .toList());
        Map<UUID, MissedVisitRecord> missedVisits = latestMissedVisitRecords(
                actorMembership.getAgencyId(),
                visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                        .map(VisitOccurrence::getId)
                        .toList());
        return visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(actorMembership.getAgencyId()).stream()
                .filter(visit -> localDate(visit.getPlannedStartAt(), visit.getTimezone()).isEqual(snapshotDate))
                .filter(visit -> branchId == null || Objects.equals(branchId, visit.getBranchId()))
                .filter(visit -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, visit.getBranchId()))
                .map(visit -> toOperationalDrilldownItem(visit, assignments.get(visit.getId()), verifications.get(visit.getId()), missedVisits.get(visit.getId())))
                .filter(item -> matchesOperationalMetric(metricType, item))
                .filter(item -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(item.detailStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BacklogDrilldownItem> listBacklogDrilldown(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            String status) {
        requirePermission(actorMembership, AgencyPermission.VIEW_QA_BACKLOG_METRICS, "view analytics backlog drilldown");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_QA_BACKLOG_METRICS, branchId, "view analytics backlog drilldown");
        }
        String normalizedStatus = normalizeOptional(status);
        return reviewWorkItemRepository.findAllByAgency_IdOrderByEnteredQueueAtDesc(actorMembership.getAgencyId()).stream()
                .filter(item -> item.getSourceType() == ReviewSourceType.VISIT_DOCUMENTATION_RECORD)
                .filter(item -> QA_BACKLOG_STATUSES.contains(item.getStatus()))
                .filter(item -> branchId == null || Objects.equals(branchId, item.getBranchId()))
                .filter(item -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_QA_BACKLOG_METRICS, item.getBranchId()))
                .map(item -> new BacklogDrilldownItem(
                        item.getId(),
                        item.getSourceRecordId(),
                        item.getPatientId(),
                        item.getBranchId(),
                        item.getStatus().name(),
                        item.getPriority() == null ? null : item.getPriority().name(),
                        item.getEnteredQueueAt(),
                        item.getDueAt(),
                        item.getDueAt() != null && item.getDueAt().isBefore(snapshotDate.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()))))
                .filter(item -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(item.status()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReadinessDrilldownItem> listRevenueReadinessDrilldown(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            RevenueReadinessStatus readinessStatus) {
        requirePermission(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_METRICS, "view analytics revenue drilldown");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_METRICS, branchId, "view analytics revenue drilldown");
        }
        return revenueReadinessProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(projection -> projection.getVisitOccurrence() != null)
                .filter(projection -> localDate(projection.getVisitOccurrence().getPlannedStartAt(), projection.getVisitOccurrence().getTimezone()).isEqual(snapshotDate))
                .filter(projection -> branchId == null || Objects.equals(branchId, projection.getBranchId()))
                .filter(projection -> readinessStatus == null || projection.getReadinessStatus() == readinessStatus)
                .filter(projection -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_REVENUE_READINESS_METRICS, projection.getBranchId()))
                .map(projection -> new ReadinessDrilldownItem(
                        projection.getVisitOccurrenceId(),
                        projection.getPatient().getId(),
                        projection.getBranchId(),
                        projection.getReadinessStatus().name(),
                        projection.getExceptionCount(),
                        projection.getWarningCount(),
                        projection.getEvaluatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplianceDrilldownItem> listComplianceDrilldown(
            @NotNull AgencyMembership actorMembership,
            UUID branchId,
            ComplianceReadinessStatus readinessStatus) {
        requirePermission(actorMembership, AgencyPermission.VIEW_COMPLIANCE_EXCEPTION_METRICS, "view analytics compliance drilldown");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_EXCEPTION_METRICS, branchId, "view analytics compliance drilldown");
        }
        Map<String, ComplianceStatusProjection> latestByPatientScope = new LinkedHashMap<>();
        for (ComplianceStatusProjection projection : complianceStatusProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(actorMembership.getAgencyId())) {
            if (branchId != null && !Objects.equals(branchId, projection.getBranchId())) {
                continue;
            }
            if (readinessStatus != null && projection.getReadinessStatus() != readinessStatus) {
                continue;
            }
            if (!hasBranchAccess(actorMembership, AgencyPermission.VIEW_COMPLIANCE_EXCEPTION_METRICS, projection.getBranchId())) {
                continue;
            }
            String key = projection.getPatientId() + "|" + Objects.toString(projection.getBranchId(), "null") + "|" + Objects.toString(projection.getServiceLineId(), "null");
            latestByPatientScope.putIfAbsent(key, projection);
        }
        return latestByPatientScope.values().stream()
                .map(projection -> new ComplianceDrilldownItem(
                        projection.getPatientId(),
                        projection.getBranchId(),
                        projection.getServiceLineId(),
                        projection.getReadinessStatus().name(),
                        projection.getCertificationPeriodStatus().name(),
                        projection.getActiveRiskReminderCount(),
                        projection.getDocumentationUnsatisfiedCount() + projection.getMissingAcknowledgmentCount() + projection.getExpiredAcknowledgmentCount(),
                        projection.getEvaluatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MetricTrendSnapshot> listMetricTrendSnapshots(
            @NotNull AgencyMembership actorMembership,
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            AnalyticsDashboardMetricType metricType) {
        requirePermission(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, "view analytics trend snapshots");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, branchId, "view analytics trend snapshots");
        }
        return metricTrendSnapshotRepository.findAllByAgency_IdAndSnapshotDateOrderByCalculatedAtDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(snapshot -> branchId == null || Objects.equals(branchId, snapshot.getBranchId()))
                .filter(snapshot -> metricType == null || snapshot.getMetricType() == metricType)
                .filter(snapshot -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, snapshot.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardSnapshot> listDashboardSnapshotHistory(
            @NotNull AgencyMembership actorMembership,
            LocalDate fromDate,
            LocalDate toDate,
            UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, "view analytics dashboard history");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, branchId, "view analytics dashboard history");
        }
        return dashboardSnapshotRepository.findAllByAgency_IdOrderBySnapshotDateDescGeneratedAtDesc(actorMembership.getAgencyId()).stream()
                .filter(snapshot -> fromDate == null || !snapshot.getSnapshotDate().isBefore(fromDate))
                .filter(snapshot -> toDate == null || !snapshot.getSnapshotDate().isAfter(toDate))
                .filter(snapshot -> branchId == null || Objects.equals(branchId, snapshot.getBranchId()))
                .filter(snapshot -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, snapshot.getBranchId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listMetricRefreshHistory(@NotNull AgencyMembership actorMembership, UUID branchId) {
        requirePermission(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, "view analytics refresh history");
        if (branchId != null) {
            requireBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, branchId, "view analytics refresh history");
        }
        Set<String> actionTypes = Set.of(
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.METRIC_REFRESH_TRIGGERED.actionType(),
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType(),
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.BRANCH_PERFORMANCE_REFRESHED.actionType(),
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.UTILIZATION_SUMMARY_REFRESHED.actionType(),
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.QA_BACKLOG_SUMMARY_REFRESHED.actionType(),
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.REVENUE_READINESS_SUMMARY_REFRESHED.actionType(),
                com.homehealthcare.analytics.foundation.Epic15AnalyticsAuditAction.COMPLIANCE_SUMMARY_REFRESHED.actionType());
        return auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(actorMembership.getAgencyId()).stream()
                .filter(event -> actionTypes.contains(event.getActionType()))
                .filter(event -> branchId == null || Objects.equals(branchId, event.getBranchId()))
                .filter(event -> hasBranchAccess(actorMembership, AgencyPermission.VIEW_ANALYTICS_WORKSPACE, event.getBranchId()))
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt).reversed())
                .toList();
    }

    private List<Branch> resolveBranchesInScope(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (branchId != null) {
            requireBranchAccess(actorMembership, permission, branchId, action);
            return List.of(resolveBranch(actorMembership.getAgencyId(), branchId));
        }
        if (permission.isAgencyWideFor(actorMembership.getRole())) {
            return branchRepository.findAllByAgency_IdOrderByNameAsc(actorMembership.getAgencyId());
        }
        List<Branch> branches = branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                actorMembership.getId(),
                BranchAssignmentStatus.ACTIVE).stream().map(BranchAssignment::getBranch).toList();
        if (branches.isEmpty()) {
            throw new UnauthorizedAnalyticsActorException(actorMembership.getId(), action);
        }
        return branches;
    }

    private Map<UUID, List<VisitOccurrence>> visitsByBranch(UUID agencyId, List<Branch> branches, LocalDate snapshotDate) {
        Set<UUID> branchIds = branches.stream().map(Branch::getId).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(agencyId).stream()
                .filter(visit -> !visit.getStatus().equals(SchedulingVisitStatus.CANCELLED))
                .filter(visit -> localDate(visit.getPlannedStartAt(), visit.getTimezone()).isEqual(snapshotDate)
                        || localDate(visit.getPlannedEndAt(), visit.getTimezone()).isEqual(snapshotDate)
                        || localDate(visit.getPlannedEndAt(), visit.getTimezone()).isBefore(snapshotDate))
                .filter(visit -> visit.getBranchId() != null && branchIds.contains(visit.getBranchId()))
                .collect(java.util.stream.Collectors.groupingBy(VisitOccurrence::getBranchId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
    }

    private BranchAnalytics buildBranchAnalytics(
            UUID agencyId,
            Branch branch,
            List<VisitOccurrence> candidateVisits,
            LocalDate snapshotDate,
            OffsetDateTime evaluatedAt,
            int lateStartThresholdMinutes) {
        List<VisitOccurrence> todaysVisits = candidateVisits.stream()
                .filter(visit -> localDate(visit.getPlannedStartAt(), visit.getTimezone()).isEqual(snapshotDate))
                .toList();
        List<UUID> todayVisitIds = todaysVisits.stream().map(VisitOccurrence::getId).toList();
        Map<UUID, CaregiverVisitAssignment> activeAssignmentsByVisit = latestActiveAssignments(agencyId, todayVisitIds);
        Map<UUID, MobileVisitExecutionSession> latestExecutionByVisit = latestExecutionSessions(todayVisitIds);
        Map<UUID, EvvVerificationSession> latestVerificationByVisit = latestVerificationSessions(todayVisitIds);
        Map<UUID, MissedVisitRecord> activeMissedVisitByVisit = latestMissedVisitRecords(agencyId, todayVisitIds);

        List<UUID> unfilledVisits = todaysVisits.stream()
                .filter(visit -> visit.getStatus() == SchedulingVisitStatus.OPEN_SHIFT || !activeAssignmentsByVisit.containsKey(visit.getId()))
                .map(VisitOccurrence::getId)
                .toList();
        List<UUID> lateStartVisits = todaysVisits.stream()
                .filter(visit -> {
                    EvvVerificationSession session = latestVerificationByVisit.get(visit.getId());
                    return session != null && session.getOpenedAt().isAfter(visit.getPlannedStartAt().plusMinutes(Math.max(0, lateStartThresholdMinutes)));
                })
                .map(VisitOccurrence::getId)
                .toList();
        List<UUID> missedVisits = todaysVisits.stream()
                .filter(visit -> activeMissedVisitByVisit.containsKey(visit.getId()))
                .map(VisitOccurrence::getId)
                .toList();

        DocumentationAgingMetrics documentationAgingMetrics = buildDocumentationAgingMetrics(agencyId, branch, candidateVisits, snapshotDate);
        QaBacklogMetrics qaBacklogMetrics = buildQaBacklogMetrics(agencyId, branch, evaluatedAt);
        List<UtilizationRow> utilizationRows = buildUtilizationMetrics(agencyId, branch, todaysVisits, activeAssignmentsByVisit, latestExecutionByVisit, snapshotDate);
        ReadinessMetrics readinessMetrics = buildReadinessMetrics(agencyId, branch, snapshotDate);

        return new BranchAnalytics(
                todayVisitIds,
                unfilledVisits,
                lateStartVisits,
                missedVisits,
                documentationAgingMetrics,
                qaBacklogMetrics,
                utilizationRows,
                readinessMetrics);
    }

    private DocumentationAgingMetrics buildDocumentationAgingMetrics(UUID agencyId, Branch branch, List<VisitOccurrence> candidateVisits, LocalDate snapshotDate) {
        Map<UUID, VisitDocumentationRecord> latestDocumentationByVisit = latestDocumentationByVisit(agencyId);
        List<UUID> agingVisitIds = new ArrayList<>();
        int zeroToOne = 0;
        int twoToThree = 0;
        int fourPlus = 0;
        for (VisitOccurrence visit : candidateVisits) {
            if (!Objects.equals(branch.getId(), visit.getBranchId())) {
                continue;
            }
            LocalDate visitCompletionDate = localDate(visit.getPlannedEndAt(), visit.getTimezone());
            if (!visitCompletionDate.isBefore(snapshotDate)) {
                continue;
            }
            VisitDocumentationRecord latestRecord = latestDocumentationByVisit.get(visit.getId());
            if (latestRecord != null && COMPLETED_DOCUMENTATION_STATUSES.contains(latestRecord.getStatus())) {
                continue;
            }
            OffsetDateTime anchor = latestRecord == null ? visit.getPlannedEndAt() : latestRecord.getLastSavedAt();
            long ageDays = Math.max(0, ChronoUnit.DAYS.between(anchor.toLocalDate(), snapshotDate));
            agingVisitIds.add(visit.getId());
            if (ageDays <= 1) {
                zeroToOne++;
            } else if (ageDays <= 3) {
                twoToThree++;
            } else {
                fourPlus++;
            }
        }
        Map<String, Integer> buckets = Map.of(
                "days_0_1", zeroToOne,
                "days_2_3", twoToThree,
                "days_4_plus", fourPlus);
        return new DocumentationAgingMetrics(agingVisitIds, buckets);
    }

    private QaBacklogMetrics buildQaBacklogMetrics(UUID agencyId, Branch branch, OffsetDateTime evaluatedAt) {
        List<ReviewWorkItem> items = reviewWorkItemRepository.findAllByAgency_IdOrderByEnteredQueueAtDesc(agencyId).stream()
                .filter(item -> item.getSourceType() == ReviewSourceType.VISIT_DOCUMENTATION_RECORD)
                .filter(item -> Objects.equals(branch.getId(), item.getBranchId()))
                .filter(item -> QA_BACKLOG_STATUSES.contains(item.getStatus()))
                .toList();
        List<UUID> itemIds = items.stream().map(ReviewWorkItem::getId).toList();
        int returned = (int) items.stream()
                .filter(item -> item.getStatus() == ReviewLifecycleStatus.RETURNED_FOR_FIX || item.getStatus() == ReviewLifecycleStatus.RESUBMITTED)
                .count();
        int overdue = (int) items.stream()
                .filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(evaluatedAt))
                .count();
        return new QaBacklogMetrics(itemIds, returned, overdue);
    }

    private List<UtilizationRow> buildUtilizationMetrics(
            UUID agencyId,
            Branch branch,
            List<VisitOccurrence> todaysVisits,
            Map<UUID, CaregiverVisitAssignment> activeAssignmentsByVisit,
            Map<UUID, MobileVisitExecutionSession> latestExecutionByVisit,
            LocalDate snapshotDate) {
        Map<UUID, List<VisitOccurrence>> visitsByCaregiver = new LinkedHashMap<>();
        for (VisitOccurrence visit : todaysVisits) {
            CaregiverVisitAssignment assignment = activeAssignmentsByVisit.get(visit.getId());
            if (assignment == null || assignment.getCaregiverProfile() == null) {
                continue;
            }
            visitsByCaregiver.computeIfAbsent(assignment.getCaregiverProfileId(), ignored -> new ArrayList<>()).add(visit);
        }
        List<UtilizationRow> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<VisitOccurrence>> entry : visitsByCaregiver.entrySet()) {
            CaregiverProfile caregiverProfile = caregiverProfileRepository.findByIdAndAgency_Id(entry.getKey(), agencyId)
                    .orElseThrow(() -> new AnalyticsEntityNotFoundException("CaregiverProfile", entry.getKey()));
            int assignedCount = entry.getValue().size();
            int completedCount = (int) entry.getValue().stream()
                    .filter(visit -> {
                        MobileVisitExecutionSession session = latestExecutionByVisit.get(visit.getId());
                        return session != null && session.getExecutionStatus() == MobileExecutionSessionStatus.COMPLETED;
                    })
                    .count();
            int scheduledMinutes = entry.getValue().stream()
                    .mapToInt(visit -> Math.toIntExact(ChronoUnit.MINUTES.between(visit.getPlannedStartAt(), visit.getPlannedEndAt())))
                    .sum();
            rows.add(new UtilizationRow(
                    caregiverProfile,
                    branch,
                    snapshotDate,
                    assignedCount,
                    completedCount,
                    scheduledMinutes,
                    deriveUtilizationPosture(assignedCount, scheduledMinutes)));
        }
        return rows;
    }

    private ReadinessMetrics buildReadinessMetrics(UUID agencyId, Branch branch, LocalDate snapshotDate) {
        int revenueReady = 0;
        int revenueWarning = 0;
        int revenueBlocked = 0;
        for (RevenueReadinessProjection projection : revenueReadinessProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(agencyId)) {
            if (!Objects.equals(branch.getId(), projection.getBranchId())) {
                continue;
            }
            if (projection.getVisitOccurrence() == null || !localDate(projection.getVisitOccurrence().getPlannedStartAt(), projection.getVisitOccurrence().getTimezone()).isEqual(snapshotDate)) {
                continue;
            }
            if (projection.getReadinessStatus() == RevenueReadinessStatus.READY || projection.getReadinessStatus() == RevenueReadinessStatus.EXPORTED) {
                revenueReady++;
            } else if (projection.getReadinessStatus() == RevenueReadinessStatus.WARNING) {
                revenueWarning++;
            } else if (projection.getReadinessStatus() == RevenueReadinessStatus.BLOCKED) {
                revenueBlocked++;
            }
        }

        Map<String, ComplianceStatusProjection> latestByPatientScope = new LinkedHashMap<>();
        for (ComplianceStatusProjection projection : complianceStatusProjectionRepository.findAllByAgency_IdOrderByEvaluatedAtDesc(agencyId)) {
            if (!Objects.equals(branch.getId(), projection.getBranchId())) {
                continue;
            }
            String key = projection.getPatientId() + "|" + Objects.toString(projection.getBranchId(), "null") + "|" + Objects.toString(projection.getServiceLineId(), "null");
            latestByPatientScope.putIfAbsent(key, projection);
        }
        int complianceReady = 0;
        int complianceWarning = 0;
        int complianceExceptions = 0;
        for (ComplianceStatusProjection projection : latestByPatientScope.values()) {
            if (projection.getReadinessStatus() == ComplianceReadinessStatus.READY) {
                complianceReady++;
            } else if (projection.getReadinessStatus() == ComplianceReadinessStatus.WARNING) {
                complianceWarning++;
                complianceExceptions++;
            } else if (projection.getReadinessStatus() == ComplianceReadinessStatus.NON_COMPLIANT) {
                complianceExceptions++;
            }
        }
        return new ReadinessMetrics(revenueReady, revenueWarning, revenueBlocked, complianceReady, complianceWarning, complianceExceptions);
    }

    private List<DashboardMetricSnapshot> saveBranchMetricSnapshots(Branch branch, LocalDate snapshotDate, OffsetDateTime evaluatedAt, BranchAnalytics analytics) {
        List<DashboardMetricSnapshot> results = new ArrayList<>();
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.TODAYS_VISITS, AnalyticsMetricScope.BRANCH, analytics.todaysVisitIds().size(), analytics.completedTodayCount(), "SCHEDULED_LOAD", analytics.todaysVisitIds()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.UNFILLED_VISITS, AnalyticsMetricScope.BRANCH, analytics.unfilledVisitIds().size(), 0, "UNFILLED", analytics.unfilledVisitIds()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.LATE_STARTS, AnalyticsMetricScope.BRANCH, analytics.lateStartVisitIds().size(), 0, "LATE_START", analytics.lateStartVisitIds()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.MISSED_VISITS, AnalyticsMetricScope.BRANCH, analytics.missedVisitIds().size(), 0, "MISSED_VISIT", analytics.missedVisitIds()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.DOCUMENTATION_AGING, AnalyticsMetricScope.BRANCH, analytics.documentationAgingMetrics().agingVisitIds().size(), analytics.qaBacklogMetrics().pendingReviewCount(), "BACKLOG", analytics.documentationAgingMetrics().agingVisitIds()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.QA_BACKLOG, AnalyticsMetricScope.BRANCH, analytics.qaBacklogMetrics().pendingReviewCount(), analytics.qaBacklogMetrics().overdueReviewCount(), "REVIEW_QUEUE", analytics.qaBacklogMetrics().reviewWorkItemIds()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.CAREGIVER_UTILIZATION, AnalyticsMetricScope.CAREGIVER, analytics.utilizationRows().size(), analytics.utilizationRows().stream().mapToInt(UtilizationRow::assignedVisitCount).sum(), "UTILIZATION", analytics.utilizationRows().stream().map(row -> row.caregiverProfile().getId()).toList()));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.REVENUE_READINESS, AnalyticsMetricScope.BRANCH, analytics.readinessMetrics().revenueReadyCount(), analytics.readinessMetrics().revenueBlockedCount(), "REVENUE", List.of(branch.getId())));
        results.add(saveMetricSnapshot(branch, snapshotDate, evaluatedAt, AnalyticsDashboardMetricType.COMPLIANCE_EXCEPTIONS, AnalyticsMetricScope.BRANCH, analytics.readinessMetrics().complianceExceptionCount(), analytics.readinessMetrics().complianceWarningCount(), "COMPLIANCE", List.of(branch.getId())));
        return results;
    }

    private DashboardMetricSnapshot saveMetricSnapshot(
            Branch branch,
            LocalDate snapshotDate,
            OffsetDateTime evaluatedAt,
            AnalyticsDashboardMetricType metricType,
            AnalyticsMetricScope metricScope,
            int primaryValue,
            int secondaryValue,
            String statusCode,
            Collection<UUID> drilldownIds) {
        DashboardMetricDefinition definition = ensureMetricDefinition(branch.getAgency(), metricType, metricScope);
        DashboardMetricSnapshot snapshot = dashboardMetricSnapshotRepository.findAllByAgency_IdAndSnapshotDateOrderByCapturedAtDesc(branch.getAgencyId(), snapshotDate).stream()
                .filter(existing -> existing.getMetricDefinition().getMetricType() == metricType)
                .filter(existing -> Objects.equals(existing.getBranchId(), branch.getId()))
                .findFirst()
                .orElseGet(() -> DashboardMetricSnapshot.create(
                        definition,
                        branch,
                        snapshotDate,
                        primaryValue,
                        secondaryValue,
                        statusCode,
                        toJson(Map.of("ids", drilldownIds)),
                        evaluatedAt));
        snapshot.recalculate(branch, primaryValue, secondaryValue, statusCode, toJson(Map.of("ids", drilldownIds)), evaluatedAt);
        return dashboardMetricSnapshotRepository.saveAndFlush(snapshot);
    }

    private BranchPerformanceSummary saveBranchPerformanceSummary(AgencyMembership actorMembership, Branch branch, LocalDate snapshotDate, OffsetDateTime evaluatedAt, BranchAnalytics analytics) {
        BranchPerformanceSummary summary = branchPerformanceSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(branch.getAgencyId(), snapshotDate).stream()
                .filter(existing -> Objects.equals(existing.getBranchId(), branch.getId()))
                .findFirst()
                .orElseGet(() -> BranchPerformanceSummary.create(
                        branch,
                        snapshotDate,
                        analytics.todaysVisitIds().size(),
                        analytics.unfilledVisitIds().size(),
                        analytics.lateStartVisitIds().size(),
                        analytics.missedVisitIds().size(),
                        analytics.documentationAgingMetrics().agingVisitIds().size(),
                        analytics.qaBacklogMetrics().pendingReviewCount(),
                        deriveBranchPerformancePosture(analytics),
                        evaluatedAt));
        summary.recalculate(
                analytics.todaysVisitIds().size(),
                analytics.unfilledVisitIds().size(),
                analytics.lateStartVisitIds().size(),
                analytics.missedVisitIds().size(),
                analytics.documentationAgingMetrics().agingVisitIds().size(),
                analytics.qaBacklogMetrics().pendingReviewCount(),
                deriveBranchPerformancePosture(analytics),
                evaluatedAt);
        BranchPerformanceSummary saved = branchPerformanceSummaryRepository.saveAndFlush(summary);
        analyticsAuditService.recordBranchPerformanceRefreshed(
                actorMembership,
                saved.getId(),
                branch.getId(),
                "{\"snapshotDate\":\"" + snapshotDate + "\"}");
        return saved;
    }

    private List<UtilizationSummary> saveUtilizationSummaries(AgencyMembership actorMembership, Branch branch, LocalDate snapshotDate, OffsetDateTime evaluatedAt, List<UtilizationRow> rows) {
        List<UtilizationSummary> saved = new ArrayList<>();
        List<UtilizationSummary> existing = utilizationSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByScheduledMinutesDesc(branch.getAgencyId(), snapshotDate);
        for (UtilizationRow row : rows) {
            UtilizationSummary summary = existing.stream()
                    .filter(candidate -> Objects.equals(candidate.getBranchId(), branch.getId()))
                    .filter(candidate -> Objects.equals(candidate.getCaregiverProfileId(), row.caregiverProfile().getId()))
                    .findFirst()
                    .orElseGet(() -> UtilizationSummary.create(
                            branch,
                            row.caregiverProfile(),
                            snapshotDate,
                            row.assignedVisitCount(),
                            row.completedVisitCount(),
                            row.scheduledMinutes(),
                            row.posture(),
                            evaluatedAt));
            summary.recalculate(branch, row.assignedVisitCount(), row.completedVisitCount(), row.scheduledMinutes(), row.posture(), evaluatedAt);
            UtilizationSummary stored = utilizationSummaryRepository.saveAndFlush(summary);
            analyticsAuditService.recordUtilizationSummaryRefreshed(
                    actorMembership,
                    stored.getId(),
                    branch.getId(),
                    "{\"caregiverProfileId\":\"" + row.caregiverProfile().getId() + "\"}");
            saved.add(stored);
        }
        return saved;
    }

    private BacklogSummary saveBacklogSummary(AgencyMembership actorMembership, Branch branch, LocalDate snapshotDate, OffsetDateTime evaluatedAt, BranchAnalytics analytics) {
        BacklogSummary summary = backlogSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(branch.getAgencyId(), snapshotDate).stream()
                .filter(existing -> Objects.equals(existing.getBranchId(), branch.getId()))
                .findFirst()
                .orElseGet(() -> BacklogSummary.create(
                        branch,
                        snapshotDate,
                        analytics.documentationAgingMetrics().agingVisitIds().size(),
                        toJson(analytics.documentationAgingMetrics().agingBuckets()),
                        analytics.qaBacklogMetrics().pendingReviewCount(),
                        analytics.qaBacklogMetrics().returnedForFixCount(),
                        analytics.qaBacklogMetrics().overdueReviewCount(),
                        deriveBacklogPosture(analytics),
                        evaluatedAt));
        summary.recalculate(
                analytics.documentationAgingMetrics().agingVisitIds().size(),
                toJson(analytics.documentationAgingMetrics().agingBuckets()),
                analytics.qaBacklogMetrics().pendingReviewCount(),
                analytics.qaBacklogMetrics().returnedForFixCount(),
                analytics.qaBacklogMetrics().overdueReviewCount(),
                deriveBacklogPosture(analytics),
                evaluatedAt);
        BacklogSummary saved = backlogSummaryRepository.saveAndFlush(summary);
        analyticsAuditService.recordQaBacklogSummaryRefreshed(
                actorMembership,
                saved.getId(),
                branch.getId(),
                "{\"snapshotDate\":\"" + snapshotDate + "\"}");
        return saved;
    }

    private ReadinessComplianceSummary saveReadinessComplianceSummary(AgencyMembership actorMembership, Branch branch, LocalDate snapshotDate, OffsetDateTime evaluatedAt, BranchAnalytics analytics) {
        ReadinessComplianceSummary summary = readinessComplianceSummaryRepository.findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(branch.getAgencyId(), snapshotDate).stream()
                .filter(existing -> Objects.equals(existing.getBranchId(), branch.getId()))
                .findFirst()
                .orElseGet(() -> ReadinessComplianceSummary.create(
                        branch,
                        snapshotDate,
                        analytics.readinessMetrics().revenueReadyCount(),
                        analytics.readinessMetrics().revenueWarningCount(),
                        analytics.readinessMetrics().revenueBlockedCount(),
                        analytics.readinessMetrics().complianceReadyCount(),
                        analytics.readinessMetrics().complianceWarningCount(),
                        analytics.readinessMetrics().complianceExceptionCount(),
                        evaluatedAt));
        summary.recalculate(
                analytics.readinessMetrics().revenueReadyCount(),
                analytics.readinessMetrics().revenueWarningCount(),
                analytics.readinessMetrics().revenueBlockedCount(),
                analytics.readinessMetrics().complianceReadyCount(),
                analytics.readinessMetrics().complianceWarningCount(),
                analytics.readinessMetrics().complianceExceptionCount(),
                evaluatedAt);
        ReadinessComplianceSummary saved = readinessComplianceSummaryRepository.saveAndFlush(summary);
        analyticsAuditService.recordRevenueReadinessSummaryRefreshed(
                actorMembership,
                saved.getId(),
                branch.getId(),
                "{\"snapshotDate\":\"" + snapshotDate + "\"}");
        analyticsAuditService.recordComplianceSummaryRefreshed(
                actorMembership,
                saved.getId(),
                branch.getId(),
                "{\"snapshotDate\":\"" + snapshotDate + "\"}");
        return saved;
    }

    private DashboardSnapshot saveDashboardSnapshot(
            AgencyMembership actorMembership,
            Branch branch,
            LocalDate snapshotDate,
            OffsetDateTime evaluatedAt,
            MetricAccumulator aggregate) {
        DashboardSnapshot snapshot = dashboardSnapshotRepository.findAllByAgency_IdAndSnapshotDateOrderByGeneratedAtDesc(actorMembership.getAgencyId(), snapshotDate).stream()
                .filter(existing -> Objects.equals(existing.getBranchId(), branch == null ? null : branch.getId()))
                .findFirst()
                .orElseGet(() -> DashboardSnapshot.create(
                        actorMembership.getAgency(),
                        branch,
                        snapshotDate,
                        aggregate.todaysVisitCount,
                        aggregate.unfilledVisitCount,
                        aggregate.lateStartCount,
                        aggregate.missedVisitCount,
                        aggregate.documentationAgingCount,
                        aggregate.qaBacklogCount,
                        aggregate.caregiverUtilizationCount,
                        aggregate.revenueBlockedCount,
                        aggregate.complianceExceptionCount,
                        evaluatedAt));
        snapshot.regenerate(
                aggregate.todaysVisitCount,
                aggregate.unfilledVisitCount,
                aggregate.lateStartCount,
                aggregate.missedVisitCount,
                aggregate.documentationAgingCount,
                aggregate.qaBacklogCount,
                aggregate.caregiverUtilizationCount,
                aggregate.revenueBlockedCount,
                aggregate.complianceExceptionCount,
                evaluatedAt);
        return dashboardSnapshotRepository.saveAndFlush(snapshot);
    }

    private List<MetricTrendSnapshot> saveTrendSnapshots(
            AgencyMembership actorMembership,
            Branch branch,
            LocalDate snapshotDate,
            OffsetDateTime evaluatedAt,
            List<DashboardMetricSnapshot> currentSnapshots) {
        List<MetricTrendSnapshot> saved = new ArrayList<>();
        List<MetricTrendSnapshot> existing = metricTrendSnapshotRepository.findAllByAgency_IdAndSnapshotDateOrderByCalculatedAtDesc(actorMembership.getAgencyId(), snapshotDate);
        for (DashboardMetricSnapshot current : currentSnapshots) {
            DashboardMetricSnapshot previous = dashboardMetricSnapshotRepository
                    .findAllByAgency_IdAndMetricDefinition_MetricTypeOrderBySnapshotDateDescCapturedAtDesc(actorMembership.getAgencyId(), current.getMetricDefinition().getMetricType()).stream()
                    .filter(candidate -> Objects.equals(candidate.getBranchId(), branch == null ? null : branch.getId()))
                    .filter(candidate -> candidate.getSnapshotDate().isBefore(snapshotDate))
                    .findFirst()
                    .orElse(null);
            int previousValue = previous == null ? 0 : previous.getPrimaryValue();
            int currentValue = current.getPrimaryValue();
            int delta = currentValue - previousValue;
            AnalyticsTrendDirection direction = delta > 0 ? AnalyticsTrendDirection.UP : (delta < 0 ? AnalyticsTrendDirection.DOWN : AnalyticsTrendDirection.FLAT);
            MetricTrendSnapshot trendSnapshot = existing.stream()
                    .filter(candidate -> candidate.getMetricType() == current.getMetricDefinition().getMetricType())
                    .filter(candidate -> Objects.equals(candidate.getBranchId(), branch == null ? null : branch.getId()))
                    .findFirst()
                    .orElseGet(() -> MetricTrendSnapshot.create(
                            current.getMetricDefinition(),
                            branch,
                            snapshotDate,
                            currentValue,
                            previousValue,
                            delta,
                            direction,
                            evaluatedAt));
            trendSnapshot.recalculate(currentValue, previousValue, delta, direction, evaluatedAt);
            saved.add(metricTrendSnapshotRepository.saveAndFlush(trendSnapshot));
        }
        return saved;
    }

    private DashboardMetricDefinition ensureMetricDefinition(
            com.homehealthcare.agency.domain.Agency agency,
            AnalyticsDashboardMetricType metricType,
            AnalyticsMetricScope metricScope) {
        MetricDefinitionSpec spec = metricDefinitionSpec(metricType, metricScope);
        DashboardMetricDefinition definition = dashboardMetricDefinitionRepository.findByAgency_IdAndMetricType(agency.getId(), metricType)
                .orElseGet(() -> DashboardMetricDefinition.create(
                        agency,
                        metricType,
                        metricScope,
                        spec.metricName(),
                        spec.description(),
                        spec.sourceDomainKey(),
                        spec.refreshMode(),
                        spec.maxStalenessMinutes()));
        definition.refreshDefinition(metricScope, spec.metricName(), spec.description(), spec.sourceDomainKey(), spec.refreshMode(), spec.maxStalenessMinutes(), true);
        return dashboardMetricDefinitionRepository.saveAndFlush(definition);
    }

    private MetricDefinitionSpec metricDefinitionSpec(AnalyticsDashboardMetricType metricType, AnalyticsMetricScope metricScope) {
        return switch (metricType) {
            case TODAYS_VISITS -> new MetricDefinitionSpec("Today's Visits", "Scheduled visit count and completion posture for the dashboard day.", "SCHEDULING", AnalyticsRefreshMode.NEAR_REAL_TIME, 15);
            case UNFILLED_VISITS -> new MetricDefinitionSpec("Unfilled Visits", "Visits still lacking an active caregiver assignment or left in open-shift posture.", "SCHEDULING", AnalyticsRefreshMode.NEAR_REAL_TIME, 15);
            case LATE_STARTS -> new MetricDefinitionSpec("Late Starts", "Visits whose EVV verification start time exceeded the configured lateness threshold.", "EVV", AnalyticsRefreshMode.NEAR_REAL_TIME, 15);
            case MISSED_VISITS -> new MetricDefinitionSpec("Missed Visits", "Visits with unresolved missed-visit records.", "EVV", AnalyticsRefreshMode.NEAR_REAL_TIME, 15);
            case DOCUMENTATION_AGING -> new MetricDefinitionSpec("Documentation Aging", "Aging counts for visits whose documentation is still missing or not submitted.", "DOCUMENTATION", AnalyticsRefreshMode.SCHEDULED, 60);
            case QA_BACKLOG -> new MetricDefinitionSpec("QA Backlog", "Pending, returned, and overdue QA review items derived from the Epic 10 review queue.", "REVIEW", AnalyticsRefreshMode.SCHEDULED, 60);
            case CAREGIVER_UTILIZATION -> new MetricDefinitionSpec("Caregiver Utilization", "Assigned load, completed work, and scheduled minutes by caregiver.", "SCHEDULING", AnalyticsRefreshMode.NEAR_REAL_TIME, 30);
            case BRANCH_PERFORMANCE -> new MetricDefinitionSpec("Branch Performance", "Branch-level summary posture built from operational and backlog metrics.", "ANALYTICS", AnalyticsRefreshMode.SCHEDULED, 60);
            case REVENUE_READINESS -> new MetricDefinitionSpec("Revenue Readiness", "Ready, warning, and blocked revenue-readiness counts for the dashboard day.", "REVENUE_READINESS", AnalyticsRefreshMode.SCHEDULED, 60);
            case COMPLIANCE_EXCEPTIONS -> new MetricDefinitionSpec("Compliance Exceptions", "Warning and non-compliant patient counts from the compliance workspace.", "COMPLIANCE", AnalyticsRefreshMode.SCHEDULED, 60);
        };
    }

    private OperationalDrilldownItem toOperationalDrilldownItem(
            VisitOccurrence visit,
            CaregiverVisitAssignment assignment,
            EvvVerificationSession verificationSession,
            MissedVisitRecord missedVisitRecord) {
        String detailStatus;
        if (missedVisitRecord != null) {
            detailStatus = "MISSED_VISIT";
        } else if (assignment == null || visit.getStatus() == SchedulingVisitStatus.OPEN_SHIFT) {
            detailStatus = "UNFILLED";
        } else if (verificationSession != null && verificationSession.getOpenedAt().isAfter(visit.getPlannedStartAt())) {
            detailStatus = "LATE_START";
        } else {
            detailStatus = "SCHEDULED";
        }
        return new OperationalDrilldownItem(
                visit.getId(),
                visit.getPatient().getId(),
                visit.getBranchId(),
                assignment == null ? null : assignment.getCaregiverProfileId(),
                assignment == null || assignment.getCaregiverProfile() == null ? null : assignment.getCaregiverProfile().getDisplayName(),
                visit.getPlannedStartAt(),
                visit.getPlannedEndAt(),
                visit.getStatus().name(),
                detailStatus);
    }

    private boolean matchesOperationalMetric(AnalyticsDashboardMetricType metricType, OperationalDrilldownItem item) {
        return switch (metricType) {
            case TODAYS_VISITS -> true;
            case UNFILLED_VISITS -> "UNFILLED".equals(item.detailStatus());
            case LATE_STARTS -> "LATE_START".equals(item.detailStatus());
            case MISSED_VISITS -> "MISSED_VISIT".equals(item.detailStatus());
            default -> false;
        };
    }

    private Map<UUID, CaregiverVisitAssignment> latestActiveAssignments(UUID agencyId, Collection<UUID> visitIds) {
        Map<UUID, CaregiverVisitAssignment> assignments = new HashMap<>();
        for (CaregiverVisitAssignment assignment : caregiverVisitAssignmentRepository.findAllByAgency_IdOrderByAssignedAtAsc(agencyId)) {
            if (!visitIds.contains(assignment.getVisitOccurrenceId()) || assignment.getAssignmentStatus() != CaregiverAssignmentStatus.ACTIVE) {
                continue;
            }
            assignments.put(assignment.getVisitOccurrenceId(), assignment);
        }
        return assignments;
    }

    private Map<UUID, MobileVisitExecutionSession> latestExecutionSessions(Collection<UUID> visitIds) {
        Map<UUID, MobileVisitExecutionSession> sessions = new HashMap<>();
        for (UUID visitId : visitIds) {
            mobileVisitExecutionSessionRepository.findAllByVisitOccurrence_IdOrderByStartedAtAsc(visitId).stream()
                    .max(Comparator.comparing(MobileVisitExecutionSession::getStartedAt))
                    .ifPresent(session -> sessions.put(visitId, session));
        }
        return sessions;
    }

    private Map<UUID, EvvVerificationSession> latestVerificationSessions(Collection<UUID> visitIds) {
        Map<UUID, EvvVerificationSession> sessions = new HashMap<>();
        for (UUID visitId : visitIds) {
            evvVerificationSessionRepository.findFirstByVisitOccurrence_IdOrderByOpenedAtDesc(visitId)
                    .ifPresent(session -> sessions.put(visitId, session));
        }
        return sessions;
    }

    private Map<UUID, MissedVisitRecord> latestMissedVisitRecords(UUID agencyId, Collection<UUID> visitIds) {
        Map<UUID, MissedVisitRecord> records = new HashMap<>();
        for (MissedVisitRecord record : missedVisitRecordRepository.findAllByAgency_IdOrderByReportedAtDesc(agencyId)) {
            UUID visitOccurrenceId = record.getVisitOccurrence() == null ? null : record.getVisitOccurrence().getId();
            if (!visitIds.contains(visitOccurrenceId) || !ACTIVE_MISSED_VISIT_STATUSES.contains(record.getStatus())) {
                continue;
            }
            records.putIfAbsent(visitOccurrenceId, record);
        }
        return records;
    }

    private Map<UUID, VisitDocumentationRecord> latestDocumentationByVisit(UUID agencyId) {
        Map<UUID, VisitDocumentationRecord> records = new LinkedHashMap<>();
        for (VisitDocumentationRecord record : visitDocumentationRecordRepository.findAllByAgency_IdOrderByLastSavedAtDesc(agencyId)) {
            records.putIfAbsent(record.getVisitOccurrenceId(), record);
        }
        return records;
    }

    private AnalyticsBranchPerformancePosture deriveBranchPerformancePosture(BranchAnalytics analytics) {
        if (!analytics.missedVisitIds().isEmpty()
                || !analytics.unfilledVisitIds().isEmpty()
                || analytics.qaBacklogMetrics().overdueReviewCount() > 0
                || !analytics.documentationAgingMetrics().agingVisitIds().isEmpty()) {
            return AnalyticsBranchPerformancePosture.AT_RISK;
        }
        if (!analytics.lateStartVisitIds().isEmpty()
                || analytics.qaBacklogMetrics().pendingReviewCount() > 0
                || analytics.readinessMetrics().revenueWarningCount() > 0
                || analytics.readinessMetrics().complianceWarningCount() > 0) {
            return AnalyticsBranchPerformancePosture.WATCH;
        }
        return AnalyticsBranchPerformancePosture.STABLE;
    }

    private AnalyticsBacklogPosture deriveBacklogPosture(BranchAnalytics analytics) {
        if (analytics.qaBacklogMetrics().overdueReviewCount() > 0 || !analytics.documentationAgingMetrics().agingVisitIds().isEmpty()) {
            return AnalyticsBacklogPosture.AT_RISK;
        }
        if (analytics.qaBacklogMetrics().pendingReviewCount() > 0 || analytics.qaBacklogMetrics().returnedForFixCount() > 0) {
            return AnalyticsBacklogPosture.WATCH;
        }
        return AnalyticsBacklogPosture.CLEAR;
    }

    private AnalyticsUtilizationPosture deriveUtilizationPosture(int assignedVisitCount, int scheduledMinutes) {
        if (assignedVisitCount >= 8 || scheduledMinutes >= 480) {
            return AnalyticsUtilizationPosture.HIGH;
        }
        if (assignedVisitCount >= 4 || scheduledMinutes >= 240) {
            return AnalyticsUtilizationPosture.BALANCED;
        }
        return AnalyticsUtilizationPosture.UNDERUTILIZED;
    }

    private Branch resolveBranch(UUID agencyId, UUID branchId) {
        return branchRepository.findById(branchId)
                .filter(branch -> Objects.equals(branch.getAgencyId(), agencyId))
                .orElseThrow(() -> new AnalyticsEntityNotFoundException("Branch", branchId));
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission, String action) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                permission,
                membershipId -> new UnauthorizedAnalyticsActorException(membershipId, action));
    }

    private void requireBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId, String action) {
        if (!hasBranchAccess(actorMembership, permission, branchId)) {
            throw new UnauthorizedAnalyticsActorException(actorMembership.getId(), action);
        }
    }

    private boolean hasBranchAccess(AgencyMembership actorMembership, AgencyPermission permission, UUID branchId) {
        if (!agencyAuthorizationGuard.hasPermission(actorMembership, permission)) {
            return false;
        }
        if (branchId == null || permission.isAgencyWideFor(actorMembership.getRole())) {
            return true;
        }
        return branchAssignmentRepository.existsByAgencyMembership_IdAndBranch_IdAndStatus(
                actorMembership.getId(),
                branchId,
                BranchAssignmentStatus.ACTIVE);
    }

    private OffsetDateTime evaluatedAt(OffsetDateTime value) {
        return value == null ? OffsetDateTime.now() : value;
    }

    private LocalDate localDate(OffsetDateTime timestamp, String timezone) {
        return timestamp.atZoneSameInstant(ZoneId.of(timezone)).toLocalDate();
    }

    private String toJson(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> "\"%s\":%s".formatted(escapeJson(String.valueOf(entry.getKey())), toJson(entry.getValue())))
                    .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::toJson)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }
        if (value instanceof String string) {
            return "\"" + escapeJson(string) + "\"";
        }
        if (value instanceof UUID uuid) {
            return "\"" + uuid + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String metricRefreshMetadata(LocalDate snapshotDate, UUID branchId, int lateStartThresholdMinutes) {
        return toJson(Map.of(
                "snapshotDate", snapshotDate.toString(),
                "branchId", Objects.toString(branchId, "ALL"),
                "lateStartThresholdMinutes", lateStartThresholdMinutes));
    }

    private String snapshotMetadata(LocalDate snapshotDate, UUID branchId, DashboardSnapshot snapshot) {
        return toJson(Map.of(
                "snapshotDate", snapshotDate.toString(),
                "branchId", Objects.toString(branchId, "ALL"),
                "todaysVisits", snapshot.getTodaysVisitCount(),
                "unfilledVisits", snapshot.getUnfilledVisitCount()));
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    public record RefreshDashboardSnapshotCommand(
            @NotNull LocalDate snapshotDate,
            UUID branchId,
            OffsetDateTime evaluatedAt,
            int lateStartThresholdMinutes,
            boolean includeTrendSnapshots) {
    }

    public record DashboardAnalyticsView(
            DashboardSnapshot dashboardSnapshot,
            List<DashboardMetricSnapshot> metricSnapshots,
            List<BranchPerformanceSummary> branchPerformanceSummaries,
            List<UtilizationSummary> utilizationSummaries,
            List<BacklogSummary> backlogSummaries,
            List<ReadinessComplianceSummary> readinessComplianceSummaries,
            List<MetricTrendSnapshot> trendSnapshots) {
    }

    public record DashboardSummaryView(
            DashboardSnapshot dashboardSnapshot,
            List<BranchPerformanceSummary> branchPerformanceSummaries,
            List<BacklogSummary> backlogSummaries,
            List<ReadinessComplianceSummary> readinessComplianceSummaries) {
    }

    public record OperationalDrilldownItem(
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            UUID caregiverProfileId,
            String caregiverDisplayName,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String visitStatus,
            String detailStatus) {
    }

    public record BacklogDrilldownItem(
            UUID reviewWorkItemId,
            UUID sourceRecordId,
            UUID patientId,
            UUID branchId,
            String status,
            String priority,
            OffsetDateTime enteredQueueAt,
            OffsetDateTime dueAt,
            boolean overdue) {
    }

    public record ReadinessDrilldownItem(
            UUID visitOccurrenceId,
            UUID patientId,
            UUID branchId,
            String readinessStatus,
            int exceptionCount,
            int warningCount,
            OffsetDateTime evaluatedAt) {
    }

    public record ComplianceDrilldownItem(
            UUID patientId,
            UUID branchId,
            UUID serviceLineId,
            String readinessStatus,
            String certificationPeriodStatus,
            int activeRiskReminderCount,
            int gapCount,
            OffsetDateTime evaluatedAt) {
    }

    private record MetricDefinitionSpec(
            String metricName,
            String description,
            String sourceDomainKey,
            AnalyticsRefreshMode refreshMode,
            int maxStalenessMinutes) {
    }

    private record DocumentationAgingMetrics(
            List<UUID> agingVisitIds,
            Map<String, Integer> agingBuckets) {
    }

    private record QaBacklogMetrics(
            List<UUID> reviewWorkItemIds,
            int returnedForFixCount,
            int overdueReviewCount) {
        int pendingReviewCount() {
            return reviewWorkItemIds.size();
        }
    }

    private record UtilizationRow(
            CaregiverProfile caregiverProfile,
            Branch branch,
            LocalDate snapshotDate,
            int assignedVisitCount,
            int completedVisitCount,
            int scheduledMinutes,
            AnalyticsUtilizationPosture posture) {
    }

    private record ReadinessMetrics(
            int revenueReadyCount,
            int revenueWarningCount,
            int revenueBlockedCount,
            int complianceReadyCount,
            int complianceWarningCount,
            int complianceExceptionCount) {
    }

    private record BranchAnalytics(
            List<UUID> todaysVisitIds,
            List<UUID> unfilledVisitIds,
            List<UUID> lateStartVisitIds,
            List<UUID> missedVisitIds,
            DocumentationAgingMetrics documentationAgingMetrics,
            QaBacklogMetrics qaBacklogMetrics,
            List<UtilizationRow> utilizationRows,
            ReadinessMetrics readinessMetrics) {
        int completedTodayCount() {
            return (int) utilizationRows.stream().mapToInt(UtilizationRow::completedVisitCount).sum();
        }
    }

    private static final class MetricAccumulator {
        private int todaysVisitCount;
        private int unfilledVisitCount;
        private int lateStartCount;
        private int missedVisitCount;
        private int documentationAgingCount;
        private int qaBacklogCount;
        private int caregiverUtilizationCount;
        private int revenueBlockedCount;
        private int complianceExceptionCount;

        private void accumulate(BranchAnalytics analytics) {
            todaysVisitCount += analytics.todaysVisitIds().size();
            unfilledVisitCount += analytics.unfilledVisitIds().size();
            lateStartCount += analytics.lateStartVisitIds().size();
            missedVisitCount += analytics.missedVisitIds().size();
            documentationAgingCount += analytics.documentationAgingMetrics().agingVisitIds().size();
            qaBacklogCount += analytics.qaBacklogMetrics().pendingReviewCount();
            caregiverUtilizationCount += analytics.utilizationRows().size();
            revenueBlockedCount += analytics.readinessMetrics().revenueBlockedCount();
            complianceExceptionCount += analytics.readinessMetrics().complianceExceptionCount();
        }
    }
}
