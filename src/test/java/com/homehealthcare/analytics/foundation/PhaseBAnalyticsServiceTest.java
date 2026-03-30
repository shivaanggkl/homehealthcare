package com.homehealthcare.analytics.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.analytics.application.AnalyticsService;
import com.homehealthcare.analytics.application.AnalyticsService.RefreshDashboardSnapshotCommand;
import com.homehealthcare.analytics.application.UnauthorizedAnalyticsActorException;
import com.homehealthcare.analytics.domain.BacklogSummary;
import com.homehealthcare.analytics.domain.BranchPerformanceSummary;
import com.homehealthcare.analytics.domain.DashboardMetricSnapshot;
import com.homehealthcare.analytics.domain.ReadinessComplianceSummary;
import com.homehealthcare.analytics.domain.UtilizationSummary;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.compliance.domain.ComplianceStatusProjection;
import com.homehealthcare.compliance.domain.ComplianceStatusProjectionRepository;
import com.homehealthcare.compliance.foundation.CertificationPeriodStatus;
import com.homehealthcare.compliance.foundation.ComplianceReadinessStatus;
import com.homehealthcare.documentation.application.DocumentationService;
import com.homehealthcare.documentation.application.DocumentationService.CreateDocumentationRecordCommand;
import com.homehealthcare.documentation.application.DocumentationService.FieldDefinitionCommand;
import com.homehealthcare.documentation.application.DocumentationService.FieldResponseCommand;
import com.homehealthcare.documentation.application.DocumentationService.ManageTemplateCommand;
import com.homehealthcare.documentation.application.DocumentationService.SaveDocumentationDraftCommand;
import com.homehealthcare.documentation.application.DocumentationService.SectionCommand;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentation.foundation.DocumentationFieldType;
import com.homehealthcare.documentation.foundation.DocumentationResponseState;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.evv.application.EvvVerificationService;
import com.homehealthcare.evv.application.EvvVerificationService.ReportMissedVisitCommand;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjection;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjectionRepository;
import com.homehealthcare.revenuereadiness.foundation.RevenueExportLifecycleStatus;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import com.homehealthcare.review.domain.ReviewWorkItem;
import com.homehealthcare.review.domain.ReviewWorkItemRepository;
import com.homehealthcare.review.foundation.ReviewPriority;
import com.homehealthcare.review.foundation.ReviewSourceType;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PhaseBAnalyticsServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired
    private BranchAssignmentRepository branchAssignmentRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ServiceLineRepository serviceLineRepository;
    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private DocumentationService documentationService;
    @Autowired
    private EvvVerificationService evvVerificationService;
    @Autowired
    private ReviewWorkItemRepository reviewWorkItemRepository;
    @Autowired
    private RevenueReadinessProjectionRepository revenueReadinessProjectionRepository;
    @Autowired
    private ComplianceStatusProjectionRepository complianceStatusProjectionRepository;
    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void analyticsPhaseBBuildsMetricSnapshotsBranchPerformanceBacklogUtilizationAndReadinessSummaries() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Analytics Home Health",
                "analytics-home-health",
                "America/Chicago",
                "ops@analytics.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Ops", "NOPS", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@analytics.example");
        AgencyMembership branchAdmin = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "branch-admin@analytics.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@analytics.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(branchAdmin, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiverMembership, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-AN-1",
                "Avery",
                null,
                "Analytics",
                null,
                LocalDate.of(1952, 5, 10),
                "F",
                "312-555-0100",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine SN", "SN-R", "Routine skilled nursing", 60, true, 1));
        CaregiverProfile caregiver = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                branch,
                "CG-AN-1",
                "Casey Analytics",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));

        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Analytics Visit Note",
                "AN-NOTE",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                branch.getId(),
                "Analytics note",
                Set.of(AgencyRole.CAREGIVER, AgencyRole.BRANCH_ADMIN, AgencyRole.QA_CLINICAL_REVIEWER),
                false,
                List.of(new SectionCommand("visit", "Visit", "Visit", 1)),
                List.of(new FieldDefinitionCommand(
                        "visit",
                        "narrative",
                        "Narrative",
                        DocumentationFieldType.LONG_TEXT,
                        true,
                        1,
                        null,
                        null,
                        Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER),
                        Set.of(AgencyRole.CAREGIVER))),
                List.of()));

        var lateVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-08-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-08-10T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Late visit"));
        schedulingRecordService.assignCaregiver(owner, lateVisit.getId(), new AssignCaregiverCommand(caregiver.getId(), branch.getId(), "board", "assign"));
        evvVerificationService.openVerificationSession(
                caregiverMembership,
                lateVisit.getId(),
                null,
                OffsetDateTime.parse("2026-08-10T09:25:00-05:00"));

        var unfilledVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-08-10T11:00:00-05:00"),
                OffsetDateTime.parse("2026-08-10T12:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Unfilled visit"));

        var missedVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-08-10T14:00:00-05:00"),
                OffsetDateTime.parse("2026-08-10T15:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Missed visit"));
        schedulingRecordService.assignCaregiver(owner, missedVisit.getId(), new AssignCaregiverCommand(caregiver.getId(), branch.getId(), "board", "assign"));
        evvVerificationService.reportMissedVisit(caregiverMembership, missedVisit.getId(), new ReportMissedVisitCommand(
                "CAREGIVER_NO_SHOW",
                "Caregiver could not complete the visit.",
                OffsetDateTime.parse("2026-08-10T14:10:00-05:00")));

        var agedVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-08-07T09:00:00-05:00"),
                OffsetDateTime.parse("2026-08-07T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Aged documentation visit"));
        VisitDocumentationRecord agedDocumentation = documentationService.createDocumentationRecord(
                caregiverMembership,
                new CreateDocumentationRecordCommand(
                        agedVisit.getId(),
                        templateAggregate.template().getId(),
                        OffsetDateTime.parse("2026-08-07T09:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiverMembership, agedDocumentation.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(0).getId(),
                        "{\"text\":\"Draft only.\"}",
                        "Draft only.",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-08-07T09:30:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-08-07T09:30:00-05:00")));

        reviewWorkItemRepository.saveAndFlush(ReviewWorkItem.queue(
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                agedDocumentation.getId(),
                branch,
                patient,
                agedVisit,
                agedDocumentation,
                ReviewPriority.HIGH,
                false,
                OffsetDateTime.parse("2026-08-08T08:00:00-05:00"),
                OffsetDateTime.parse("2026-08-09T12:00:00-05:00")));

        revenueReadinessProjectionRepository.saveAndFlush(RevenueReadinessProjection.create(
                lateVisit,
                patient,
                branch,
                serviceLine,
                null,
                null,
                null,
                null,
                RevenueReadinessStatus.WARNING,
                0,
                1,
                RevenueExportLifecycleStatus.NOT_REQUESTED,
                OffsetDateTime.parse("2026-08-10T18:00:00-05:00")));
        revenueReadinessProjectionRepository.saveAndFlush(RevenueReadinessProjection.create(
                missedVisit,
                patient,
                branch,
                serviceLine,
                null,
                null,
                null,
                null,
                RevenueReadinessStatus.BLOCKED,
                1,
                0,
                RevenueExportLifecycleStatus.NOT_REQUESTED,
                OffsetDateTime.parse("2026-08-10T18:05:00-05:00")));

        complianceStatusProjectionRepository.saveAndFlush(ComplianceStatusProjection.create(
                patient,
                branch,
                serviceLine,
                0,
                1,
                0,
                0,
                1,
                0,
                0,
                0,
                CertificationPeriodStatus.CURRENT,
                1,
                ComplianceReadinessStatus.WARNING,
                OffsetDateTime.parse("2026-08-10T17:00:00-05:00")));

        var analyticsView = analyticsService.refreshDashboardSnapshot(owner, new RefreshDashboardSnapshotCommand(
                LocalDate.of(2026, 8, 10),
                branch.getId(),
                OffsetDateTime.parse("2026-08-10T18:30:00-05:00"),
                15,
                true));

        assertThat(analyticsView.dashboardSnapshot().getTodaysVisitCount()).isEqualTo(3);
        assertThat(analyticsView.dashboardSnapshot().getUnfilledVisitCount()).isEqualTo(1);
        assertThat(analyticsView.dashboardSnapshot().getLateStartCount()).isEqualTo(1);
        assertThat(analyticsView.dashboardSnapshot().getMissedVisitCount()).isEqualTo(1);
        assertThat(analyticsView.dashboardSnapshot().getDocumentationAgingCount()).isEqualTo(1);
        assertThat(analyticsView.dashboardSnapshot().getQaBacklogCount()).isEqualTo(1);
        assertThat(analyticsView.dashboardSnapshot().getRevenueBlockedCount()).isEqualTo(1);
        assertThat(analyticsView.dashboardSnapshot().getComplianceExceptionCount()).isEqualTo(1);

        List<DashboardMetricSnapshot> metricSnapshots = analyticsService.listMetricSnapshots(branchAdmin, LocalDate.of(2026, 8, 10), branch.getId(), null);
        assertThat(metricSnapshots).hasSize(9);
        assertThat(metricSnapshots).anyMatch(snapshot ->
                snapshot.getMetricDefinition().getMetricType() == AnalyticsDashboardMetricType.TODAYS_VISITS
                        && snapshot.getPrimaryValue() == 3);
        assertThat(metricSnapshots).anyMatch(snapshot ->
                snapshot.getMetricDefinition().getMetricType() == AnalyticsDashboardMetricType.UNFILLED_VISITS
                        && snapshot.getPrimaryValue() == 1);

        List<BranchPerformanceSummary> branchPerformance = analyticsService.listBranchPerformanceSummaries(
                branchAdmin,
                LocalDate.of(2026, 8, 10),
                branch.getId());
        assertThat(branchPerformance).singleElement().satisfies(summary -> {
            assertThat(summary.getTodaysVisitCount()).isEqualTo(3);
            assertThat(summary.getMissedVisitCount()).isEqualTo(1);
            assertThat(summary.getPosture()).isEqualTo(AnalyticsBranchPerformancePosture.AT_RISK);
        });

        List<UtilizationSummary> utilizationSummaries = analyticsService.listUtilizationSummaries(
                branchAdmin,
                LocalDate.of(2026, 8, 10),
                branch.getId());
        assertThat(utilizationSummaries).singleElement().satisfies(summary -> {
            assertThat(summary.getAssignedVisitCount()).isEqualTo(2);
            assertThat(summary.getCompletedVisitCount()).isZero();
            assertThat(summary.getScheduledMinutes()).isEqualTo(120);
        });

        List<BacklogSummary> backlogSummaries = analyticsService.listBacklogSummaries(
                branchAdmin,
                LocalDate.of(2026, 8, 10),
                branch.getId());
        assertThat(backlogSummaries).singleElement().satisfies(summary -> {
            assertThat(summary.getDocumentationAgingCount()).isEqualTo(1);
            assertThat(summary.getPendingReviewCount()).isEqualTo(1);
            assertThat(summary.getOverdueReviewCount()).isEqualTo(1);
        });

        List<ReadinessComplianceSummary> readinessSummaries = analyticsService.listReadinessComplianceSummaries(
                branchAdmin,
                LocalDate.of(2026, 8, 10),
                branch.getId());
        assertThat(readinessSummaries).singleElement().satisfies(summary -> {
            assertThat(summary.getRevenueWarningCount()).isEqualTo(1);
            assertThat(summary.getRevenueBlockedCount()).isEqualTo(1);
            assertThat(summary.getComplianceWarningCount()).isEqualTo(1);
            assertThat(summary.getComplianceExceptionCount()).isEqualTo(1);
        });

        assertThat(analyticsView.trendSnapshots()).hasSize(9);
        assertThat(auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId())).anyMatch(event ->
                event.getActionType().equals(Epic15AnalyticsAuditAction.DASHBOARD_SNAPSHOT_GENERATED.actionType()));

        assertThatThrownBy(() -> analyticsService.refreshDashboardSnapshot(
                caregiverMembership,
                new RefreshDashboardSnapshotCommand(LocalDate.of(2026, 8, 10), branch.getId(), OffsetDateTime.now(), 15, false)))
                .isInstanceOf(UnauthorizedAnalyticsActorException.class);

        assertThat(unfilledVisit).isNotNull();
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = User.invite("Test", "User", email, null);
        user.activate();
        user = userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
