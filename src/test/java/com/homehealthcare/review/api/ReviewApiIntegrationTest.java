package com.homehealthcare.review.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
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
import com.homehealthcare.evv.application.EvvVerificationService.RecordSignatureStatusCommand;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.review.application.ReviewWorkspaceService;
import com.homehealthcare.review.application.ReviewWorkspaceService.CreateReviewWorkItemCommand;
import com.homehealthcare.review.domain.ReviewWorkItem;
import com.homehealthcare.review.foundation.ReviewExceptionType;
import com.homehealthcare.review.foundation.ReviewFindingSeverity;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.hamcrest.Matchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewApiIntegrationTest {

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
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private EvvVerificationService evvVerificationService;
    @Autowired
    private ReviewWorkspaceService reviewWorkspaceService;

    @Test
    void reviewApisSupportQueueAssignmentDecisionCompletenessExceptionQueueAndHistory() throws Exception {
        ReviewScenario scenario = createScenario();

        mockMvc.perform(get("/api/review/work-items")
                        .param("sourceType", "VISIT_DOCUMENTATION_RECORD")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].workItem.id").value(scenario.reviewWorkItem().getId().toString()))
                .andExpect(jsonPath("$.content[0].failCount").value(0));

        mockMvc.perform(post("/api/review/work-items/%s/completeness/recalculate".formatted(scenario.reviewWorkItem().getId()))
                        .param("evaluatedAt", "2026-07-01T11:00:00-05:00")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.runNumber").value(1))
                .andExpect(jsonPath("$.findings[*].ruleCode").isArray())
                .andExpect(jsonPath("$.missingFieldResults[0].fieldPath").value("bp_reading"));

        mockMvc.perform(get("/api/review/work-items/%s/missing-fields".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fieldPath").value("bp_reading"));

        mockMvc.perform(post("/api/review/work-items/%s/assignments".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerMembershipId":"%s",
                                  "assignedAt":"2026-07-01T11:05:00-05:00",
                                  "assignmentNote":"Primary QA reviewer"
                                }
                                """.formatted(scenario.reviewer().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeAssignment.reviewerMembershipId").value(scenario.reviewer().getId().toString()))
                .andExpect(jsonPath("$.workItem.status").value("ASSIGNED"));

        mockMvc.perform(post("/api/review/work-items/%s/decisions".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionType":"RETURN_FOR_FIX",
                                  "decidedAt":"2026-07-01T11:10:00-05:00",
                                  "reasonCode":"MISSING_ITEMS",
                                  "reviewerNotes":"Need BP and signature.",
                                  "returnReason":"Incomplete documentation",
                                  "requiredCorrections":"Add BP and signature"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workItem.status").value("RETURNED_FOR_FIX"))
                .andExpect(jsonPath("$.exceptions[0].exceptionType").value("RETURNED_WITH_OPEN_FINDING"));

        mockMvc.perform(post("/api/review/work-items/%s/resubmissions".formatted(scenario.reviewWorkItem().getId()))
                        .param("resubmittedAt", "2026-07-01T11:20:00-05:00")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workItem.status").value("RESUBMITTED"));

        documentationService.saveDraft(scenario.caregiver(), scenario.documentationRecord().getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        scenario.templateFieldIds().get(1),
                        "{\"text\":\"118/76\"}",
                        "118/76",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-07-01T11:18:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-07-01T11:18:00-05:00")));
        evvVerificationService.recordSignatureStatus(
                scenario.caregiver(),
                scenario.verificationSessionId(),
                new RecordSignatureStatusCommand(null, SignatureSignerRole.PATIENT, SignatureVerificationStatus.PRESENT,
                        OffsetDateTime.parse("2026-07-01T11:19:00-05:00")));

        mockMvc.perform(post("/api/review/work-items/%s/signoff-requests".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedFromMembershipId":"%s",
                                  "requestedAt":"2026-07-01T11:25:00-05:00",
                                  "signoffNote":"Need branch admin signoff."
                                }
                                """.formatted(scenario.signoffReviewer().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workItem.status").value("SIGNOFF_REQUESTED"))
                .andExpect(jsonPath("$.signoffRequests[0].requestedFromMembershipId").value(scenario.signoffReviewer().getId().toString()));

        mockMvc.perform(post("/api/review/work-items/%s/signoff-completion".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.signoffReviewer())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "completedAt":"2026-07-01T11:30:00-05:00",
                                  "approved":true,
                                  "signoffNote":"Approved."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workItem.status").value("SIGNOFF_COMPLETED"));

        mockMvc.perform(post("/api/review/work-items/%s/decisions".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.owner())))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionType":"APPROVE",
                                  "decidedAt":"2026-07-01T11:35:00-05:00",
                                  "reasonCode":"READY",
                                  "reviewerNotes":"Complete."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workItem.status").value("APPROVED"));

        mockMvc.perform(get("/api/review/work-items/%s/history".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].reviewerMembershipId").value(scenario.reviewer().getId().toString()))
                .andExpect(jsonPath("$.decisions.length()").value(2))
                .andExpect(jsonPath("$.signoffRequests[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.sourceAuditContext.length()").value(Matchers.greaterThan(0)));

        mockMvc.perform(get("/api/review/work-items/%s/decisions".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decisionType").value("RETURN_FOR_FIX"));

        mockMvc.perform(get("/api/review/work-items/%s/assignments".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentNote").value("Primary QA reviewer"));

        mockMvc.perform(get("/api/review/work-items/%s/signoff-requests".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        mockMvc.perform(get("/api/review/exception-queue")
                        .param("exceptionType", "MISSING_REQUIRED_DOCUMENTATION")
                        .with(authentication(TestTenantAuthentications.authenticationFor(scenario.reviewer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].exceptions[0].exceptionType").value("MISSING_REQUIRED_DOCUMENTATION"));
    }

    @Test
    void reviewApisEnforceForbiddenForUnassignedBranchReviewer() throws Exception {
        ReviewScenario scenario = createScenario();
        AgencyMembership outsider = createMembership(scenario.agency(), AgencyRole.QA_CLINICAL_REVIEWER, "outsider@northstar.example");

        mockMvc.perform(get("/api/review/work-items")
                        .with(authentication(TestTenantAuthentications.authenticationFor(outsider))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/review/work-items/%s".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(outsider))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/review/work-items/%s/assignments".formatted(scenario.reviewWorkItem().getId()))
                        .with(authentication(TestTenantAuthentications.authenticationFor(outsider)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewerMembershipId":"%s",
                                  "assignedAt":"2026-07-01T11:05:00-05:00"
                                }
                                """.formatted(scenario.reviewer().getId())))
                .andExpect(status().isForbidden());
    }

    private ReviewScenario createScenario() {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = createMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership reviewer = createMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@northstar.example");
        AgencyMembership signoffReviewer = createMembership(agency, AgencyRole.BRANCH_ADMIN, "signoff@northstar.example");
        AgencyMembership caregiver = createMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(reviewer, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(signoffReviewer, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiver, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-REVIEW-1",
                "Ava",
                null,
                "Patient",
                null,
                LocalDate.of(1950, 5, 20),
                "F",
                "312-555-0100",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine visit", 60, true, 1));
        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Skilled Nursing Review Note",
                "SN-REVIEW-API",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                branch.getId(),
                "Review required.",
                Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER, AgencyRole.BRANCH_ADMIN),
                true,
                List.of(new SectionCommand("overview", "Overview", "Overview", 1)),
                List.of(
                        new FieldDefinitionCommand("overview", "narrative", "Visit Narrative", DocumentationFieldType.LONG_TEXT, true, 1, null, null,
                                Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER),
                                Set.of(AgencyRole.CAREGIVER)),
                        new FieldDefinitionCommand("overview", "bp_reading", "Blood Pressure", DocumentationFieldType.TEXT, true, 2, null, null,
                                Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER),
                                Set.of(AgencyRole.CAREGIVER))),
                List.of()));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-07-01T09:00:00-05:00"),
                OffsetDateTime.parse("2026-07-01T10:00:00-05:00"),
                "America/Chicago",
                "routine",
                "manual",
                "Routine visit"));
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiver, branch, "CG-REVIEW-1", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", "assigned"));

        VisitDocumentationRecord record = documentationService.createDocumentationRecord(
                caregiver,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-07-01T09:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiver, record.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(0).getId(),
                        "{\"text\":\"Patient stable.\"}",
                        "Patient stable.",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-07-01T09:15:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-07-01T09:15:00-05:00")));

        var executionSession = mobileExecutionService.startVisitExecution(caregiver, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-07-01T09:01:00-05:00"),
                BigDecimal.valueOf(41.881),
                BigDecimal.valueOf(-87.623),
                "mobile_app",
                null));
        var verificationSession = evvVerificationService.openVerificationSession(caregiver, visit.getId(), executionSession.getId(),
                OffsetDateTime.parse("2026-07-01T09:02:00-05:00"));
        evvVerificationService.recordSignatureStatus(caregiver, verificationSession.getId(),
                new RecordSignatureStatusCommand(null, SignatureSignerRole.PATIENT, SignatureVerificationStatus.MISSING,
                        OffsetDateTime.parse("2026-07-01T09:25:00-05:00")));

        ReviewWorkItem reviewWorkItem = reviewWorkspaceService.createWorkItem(owner, new CreateReviewWorkItemCommand(
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                record.getId(),
                ReviewPriority.HIGH,
                OffsetDateTime.parse("2026-07-01T10:30:00-05:00"),
                OffsetDateTime.parse("2026-07-02T10:30:00-05:00"),
                false,
                null,
                null));

        var exceptionVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-07-01T12:00:00-05:00"),
                OffsetDateTime.parse("2026-07-01T13:00:00-05:00"),
                "America/Chicago",
                "routine",
                "manual",
                "Routine exception visit"));
        schedulingRecordService.assignCaregiver(owner, exceptionVisit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", "assigned"));
        VisitDocumentationRecord exceptionRecord = documentationService.createDocumentationRecord(
                caregiver,
                new CreateDocumentationRecordCommand(exceptionVisit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-07-01T12:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiver, exceptionRecord.getId(), new SaveDocumentationDraftCommand(
                List.of(),
                List.of(),
                OffsetDateTime.parse("2026-07-01T12:10:00-05:00")));
        reviewWorkspaceService.createWorkItem(owner, new CreateReviewWorkItemCommand(
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                exceptionRecord.getId(),
                ReviewPriority.CRITICAL,
                OffsetDateTime.parse("2026-07-01T12:35:00-05:00"),
                null,
                true,
                ReviewExceptionType.MISSING_REQUIRED_DOCUMENTATION,
                ReviewFindingSeverity.CRITICAL));

        return new ReviewScenario(agency, owner, reviewer, signoffReviewer, caregiver, reviewWorkItem, record, verificationSession.getId(),
                templateAggregate.fields().stream().map(item -> item.getId()).toList());
    }

    private AgencyMembership createMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Epic10", role.name(), email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private record ReviewScenario(
            Agency agency,
            AgencyMembership owner,
            AgencyMembership reviewer,
            AgencyMembership signoffReviewer,
            AgencyMembership caregiver,
            com.homehealthcare.review.domain.ReviewWorkItem reviewWorkItem,
            VisitDocumentationRecord documentationRecord,
            UUID verificationSessionId,
            List<UUID> templateFieldIds) {
    }
}
