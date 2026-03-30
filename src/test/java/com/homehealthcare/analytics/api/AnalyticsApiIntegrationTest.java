package com.homehealthcare.analytics.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.analytics.application.AnalyticsService;
import com.homehealthcare.analytics.application.AnalyticsService.RefreshDashboardSnapshotCommand;
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
import com.homehealthcare.testsupport.TestTenantAuthentications;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
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

    @Test
    void analyticsApisSupportDashboardDrilldownsSummariesHistoryAndRefresh() throws Exception {
        AnalyticsScenario scenario = createScenario();

        mockMvc.perform(get("/api/analytics/dashboard")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboardSnapshot.todaysVisitCount").value(3))
                .andExpect(jsonPath("$.dashboardSnapshot.unfilledVisitCount").value(1))
                .andExpect(jsonPath("$.dashboardSnapshot.missedVisitCount").value(1));

        mockMvc.perform(get("/api/analytics/metrics")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .param("metricType", "TODAYS_VISITS")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].primaryValue").value(3));

        mockMvc.perform(get("/api/analytics/drilldowns/operational")
                        .param("metricType", "MISSED_VISITS")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].visitOccurrenceId").value(scenario.missedVisitId().toString()))
                .andExpect(jsonPath("$.content[0].detailStatus").value("MISSED_VISIT"));

        mockMvc.perform(get("/api/analytics/drilldowns/backlog")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].overdue").value(true));

        mockMvc.perform(get("/api/analytics/drilldowns/revenue")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .param("readinessStatus", "BLOCKED")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].visitOccurrenceId").value(scenario.missedVisitId().toString()));

        mockMvc.perform(get("/api/analytics/drilldowns/compliance")
                        .param("branchId", scenario.branch().getId().toString())
                        .param("readinessStatus", "WARNING")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].patientId").value(scenario.patient().getId().toString()))
                .andExpect(jsonPath("$.content[0].gapCount").value(1));

        mockMvc.perform(get("/api/analytics/branch-performance")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].performancePosture").value("AT_RISK"));

        mockMvc.perform(get("/api/analytics/caregiver-utilization")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .param("caregiverProfileId", scenario.caregiver().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].assignedVisitCount").value(2));

        mockMvc.perform(get("/api/analytics/trends")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .param("metricType", "TODAYS_VISITS")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].metricType").value("TODAYS_VISITS"));

        mockMvc.perform(get("/api/analytics/history/dashboard-snapshots")
                        .param("fromDate", "2026-08-10")
                        .param("toDate", "2026-08-11")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].snapshotDate").value("2026-08-11"))
                .andExpect(jsonPath("$.content[1].snapshotDate").value("2026-08-10"));

        mockMvc.perform(get("/api/analytics/history/metric-refreshes")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
                .andExpect(jsonPath("$.content[0].actionType").exists());

        mockMvc.perform(post("/api/analytics/dashboard/refresh")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "snapshotDate":"2026-08-10",
                                  "branchId":"%s",
                                  "evaluatedAt":"2026-08-10T19:00:00-05:00",
                                  "lateStartThresholdMinutes":15,
                                  "includeTrendSnapshots":false
                                }
                                """.formatted(scenario.branch().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboardSnapshot.todaysVisitCount").value(3))
                .andExpect(jsonPath("$.trendSnapshots.length()").value(0));
    }

    @Test
    void analyticsApisEnforceForbiddenForOutOfScopeBranchUser() throws Exception {
        AnalyticsScenario scenario = createScenario();

        mockMvc.perform(get("/api/analytics/dashboard")
                        .param("snapshotDate", "2026-08-10")
                        .param("branchId", scenario.branch().getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.outOfScopeBranchAdmin()))))
                .andExpect(status().isForbidden());
    }

    private AnalyticsScenario createScenario() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Analytics API Home Health",
                "analytics-api",
                "America/Chicago",
                "ops@analytics-api.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Ops", "NOPS", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Ops", "SOPS", "Aurora", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@analytics-api.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@analytics-api.example");
        AgencyMembership outOfScopeBranchAdmin = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "branch-admin-out@analytics-api.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiverMembership, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(outOfScopeBranchAdmin, otherBranch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-AN-API-1",
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
                "CG-AN-API-1",
                "Casey Analytics",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));

        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Analytics Visit Note",
                "AN-API-NOTE",
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

        schedulingRecordService.createVisit(owner, new ManageVisitCommand(
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
                0,
                1,
                0,
                0,
                CertificationPeriodStatus.CURRENT,
                1,
                ComplianceReadinessStatus.WARNING,
                OffsetDateTime.parse("2026-08-10T17:00:00-05:00")));

        analyticsService.refreshDashboardSnapshot(owner, new RefreshDashboardSnapshotCommand(
                LocalDate.of(2026, 8, 10),
                branch.getId(),
                OffsetDateTime.parse("2026-08-10T18:30:00-05:00"),
                15,
                true));
        analyticsService.refreshDashboardSnapshot(owner, new RefreshDashboardSnapshotCommand(
                LocalDate.of(2026, 8, 11),
                branch.getId(),
                OffsetDateTime.parse("2026-08-11T08:00:00-05:00"),
                15,
                false));

        return new AnalyticsScenario(owner, outOfScopeBranchAdmin, branch, patient, caregiver, missedVisit.getId());
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = User.invite("Test", "User", email, null);
        user.activate();
        user = userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private record AnalyticsScenario(
            AgencyMembership owner,
            AgencyMembership outOfScopeBranchAdmin,
            Branch branch,
            Patient patient,
            CaregiverProfile caregiver,
            java.util.UUID missedVisitId) {
    }
}
