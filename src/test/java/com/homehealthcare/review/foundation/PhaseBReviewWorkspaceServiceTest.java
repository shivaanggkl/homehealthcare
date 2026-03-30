package com.homehealthcare.review.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.review.application.ReviewConflictException;
import com.homehealthcare.review.application.ReviewWorkspaceService;
import com.homehealthcare.review.application.ReviewWorkspaceService.AssignReviewWorkCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.CreateReviewWorkItemCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.RecordReviewDecisionCommand;
import com.homehealthcare.review.application.ReviewWorkspaceService.RequestSignoffCommand;
import com.homehealthcare.review.application.UnauthorizedReviewActorException;
import com.homehealthcare.review.domain.ReturnForFixEvent;
import com.homehealthcare.review.domain.ReviewAssignmentRepository;
import com.homehealthcare.review.domain.ReviewDecisionRepository;
import com.homehealthcare.review.domain.ReviewExceptionRecordRepository;
import com.homehealthcare.review.domain.ReviewFindingRepository;
import com.homehealthcare.review.domain.ReviewWorkItem;
import com.homehealthcare.review.domain.ReviewWorkItemRepository;
import com.homehealthcare.review.domain.SignoffRequestRepository;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
class PhaseBReviewWorkspaceServiceTest {

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
    @Autowired
    private ReviewWorkItemRepository reviewWorkItemRepository;
    @Autowired
    private ReviewFindingRepository reviewFindingRepository;
    @Autowired
    private ReviewAssignmentRepository reviewAssignmentRepository;
    @Autowired
    private ReviewDecisionRepository reviewDecisionRepository;
    @Autowired
    private ReviewExceptionRecordRepository reviewExceptionRecordRepository;
    @Autowired
    private SignoffRequestRepository signoffRequestRepository;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void reviewPhaseBSupportsQueueCompletenessAssignmentDecisionExceptionsAndSignoff() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create("North Star Home Care", "north-star-review", "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership reviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@northstar.example");
        AgencyMembership signoffReviewer = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "signoff@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(reviewer, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(signoffReviewer, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiverMembership, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-1000",
                "Ava",
                null,
                "Patient",
                null,
                LocalDate.of(1952, 4, 12),
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
                "SN-REVIEW",
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
                OffsetDateTime.parse("2026-06-01T09:00:00-05:00"),
                OffsetDateTime.parse("2026-06-01T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Routine visit"));
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-1000", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", "assigned"));

        VisitDocumentationRecord record = documentationService.createDocumentationRecord(
                caregiverMembership,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-06-01T09:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiverMembership, record.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(0).getId(),
                        "{\"text\":\"Patient stable.\"}",
                        "Patient stable.",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-06-01T09:15:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-06-01T09:15:00-05:00")));

        var executionSession = mobileExecutionService.startVisitExecution(caregiverMembership, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-06-01T09:01:00-05:00"),
                BigDecimal.valueOf(41.881),
                BigDecimal.valueOf(-87.623),
                "mobile_app",
                null));
        var verificationSession = evvVerificationService.openVerificationSession(
                caregiverMembership,
                visit.getId(),
                executionSession.getId(),
                OffsetDateTime.parse("2026-06-01T09:02:00-05:00"));
        evvVerificationService.recordSignatureStatus(caregiverMembership, verificationSession.getId(),
                new RecordSignatureStatusCommand(null, SignatureSignerRole.PATIENT, SignatureVerificationStatus.MISSING,
                        OffsetDateTime.parse("2026-06-01T09:25:00-05:00")));

        ReviewWorkItem workItem = reviewWorkspaceService.createWorkItem(owner, new CreateReviewWorkItemCommand(
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                record.getId(),
                ReviewPriority.HIGH,
                OffsetDateTime.parse("2026-06-01T10:30:00-05:00"),
                OffsetDateTime.parse("2026-06-02T10:30:00-05:00"),
                false,
                null,
                null));

        var evaluation = reviewWorkspaceService.recalculateCompleteness(reviewer, workItem.getId(), OffsetDateTime.parse("2026-06-01T10:40:00-05:00"));
        assertThat(evaluation.result().getFailCount()).isGreaterThanOrEqualTo(2);
        assertThat(evaluation.missingFieldResults()).hasSize(1);
        assertThat(evaluation.findings()).extracting("ruleCode")
                .contains("REQUIRED_FIELD_MISSING", "SIGNATURE_MISSING");

        var assignment = reviewWorkspaceService.assignWorkItem(owner, workItem.getId(), new AssignReviewWorkCommand(
                reviewer.getId(),
                OffsetDateTime.parse("2026-06-01T10:45:00-05:00"),
                "Primary reviewer"));
        assertThat(assignment.isActive()).isTrue();
        assertThat(reviewWorkItemRepository.findByIdAndAgency_Id(workItem.getId(), agency.getId()).orElseThrow().getStatus())
                .isEqualTo(ReviewLifecycleStatus.ASSIGNED);

        reviewWorkspaceService.recordDecision(reviewer, workItem.getId(), new RecordReviewDecisionCommand(
                ReviewDecisionType.RETURN_FOR_FIX,
                OffsetDateTime.parse("2026-06-01T11:00:00-05:00"),
                "MISSING_ITEMS",
                "Need BP and valid signature.",
                "Complete missing documentation elements.",
                "Capture blood pressure and signature."));
        ReturnForFixEvent returnForFixEvent = reviewWorkspaceService.markResubmitted(
                reviewer,
                workItem.getId(),
                OffsetDateTime.parse("2026-06-01T11:30:00-05:00"));
        assertThat(returnForFixEvent.getResubmittedAt()).isNotNull();

        documentationService.saveDraft(caregiverMembership, record.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(1).getId(),
                        "{\"text\":\"118/76\"}",
                        "118/76",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-06-01T11:20:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-06-01T11:20:00-05:00")));
        evvVerificationService.recordSignatureStatus(caregiverMembership, verificationSession.getId(),
                new RecordSignatureStatusCommand(null, SignatureSignerRole.PATIENT, SignatureVerificationStatus.PRESENT,
                        OffsetDateTime.parse("2026-06-01T11:25:00-05:00")));
        var secondEvaluation = reviewWorkspaceService.recalculateCompleteness(reviewer, workItem.getId(), OffsetDateTime.parse("2026-06-01T11:40:00-05:00"));
        assertThat(secondEvaluation.result().getRunNumber()).isEqualTo(2);
        assertThat(secondEvaluation.missingFieldResults()).isEmpty();

        reviewWorkspaceService.requestSignoff(reviewer, workItem.getId(), new RequestSignoffCommand(
                signoffReviewer.getId(),
                null,
                OffsetDateTime.parse("2026-06-01T11:45:00-05:00"),
                "Need branch admin signoff."));
        reviewWorkspaceService.completeSignoff(signoffReviewer, workItem.getId(), OffsetDateTime.parse("2026-06-01T11:50:00-05:00"), true, "Approved.");
        reviewWorkspaceService.recordDecision(owner, workItem.getId(), new RecordReviewDecisionCommand(
                ReviewDecisionType.APPROVE,
                OffsetDateTime.parse("2026-06-01T11:55:00-05:00"),
                "READY",
                "Documentation is complete.",
                null,
                null));

        ReviewWorkItem saved = reviewWorkItemRepository.findByIdAndAgency_Id(workItem.getId(), agency.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReviewLifecycleStatus.APPROVED);
        assertThat(reviewAssignmentRepository.findAllByWorkItem_IdOrderByAssignedAtAsc(workItem.getId())).hasSize(1);
        assertThat(reviewDecisionRepository.findAllByWorkItem_IdOrderByDecidedAtAsc(workItem.getId())).hasSize(2);
        assertThat(signoffRequestRepository.findAllByWorkItem_IdOrderByRequestedAtAsc(workItem.getId())).hasSize(1);
        assertThat(reviewFindingRepository.findAllByWorkItem_IdOrderByEvaluatedAtAscRuleCodeAsc(workItem.getId())).isNotEmpty();
        assertThat(reviewExceptionRecordRepository.findAllByWorkItem_IdOrderByDetectedAtAsc(workItem.getId())).hasSize(1);
        assertThat(auditEventRepository.findAll().stream().map(event -> event.getActionType()))
                .contains(
                        Epic10ReviewAuditAction.REVIEW_ITEM_CREATED.actionType(),
                        Epic10ReviewAuditAction.REVIEW_ASSIGNED.actionType(),
                        Epic10ReviewAuditAction.REVIEW_DECISION_RECORDED.actionType(),
                        Epic10ReviewAuditAction.REVIEW_RETURNED_FOR_FIX.actionType(),
                        Epic10ReviewAuditAction.COMPLETENESS_RECALCULATED.actionType(),
                        Epic10ReviewAuditAction.SIGNOFF_REQUESTED.actionType());
    }

    @Test
    void reviewPhaseBPreventsDuplicateActiveQueueEntriesAndEnforcesBranchScopedAuthorization() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create("North Star Home Care", "north-star-review-auth", "America/Chicago", "ops@northstar.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner-auth@northstar.example");
        AgencyMembership qaReviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer-auth@northstar.example");
        AgencyMembership foreignBranchReviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer-no-branch@northstar.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(qaReviewer, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-1001",
                "Liam",
                null,
                "Patient",
                null,
                LocalDate.of(1954, 3, 2),
                "M",
                "312-555-0200",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Follow-up", "FU", "Follow-up visit", 60, true, 1));
        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Follow-up Review Note",
                "FU-REVIEW",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                branch.getId(),
                null,
                Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER),
                false,
                List.of(new SectionCommand("overview", "Overview", "Overview", 1)),
                List.of(new FieldDefinitionCommand("overview", "summary", "Summary", DocumentationFieldType.LONG_TEXT, true, 1, null, null,
                        Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER),
                        Set.of(AgencyRole.CAREGIVER))),
                List.of()));
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver-auth@northstar.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiverMembership, branch));
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-1001", "Taylor Care", "Part Time", LocalDate.of(2026, 1, 1), null, null));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-06-03T09:00:00-05:00"),
                OffsetDateTime.parse("2026-06-03T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Routine"));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", "assigned"));
        VisitDocumentationRecord record = documentationService.createDocumentationRecord(
                caregiverMembership,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-06-03T09:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiverMembership, record.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(0).getId(),
                        "{\"text\":\"All set.\"}",
                        "All set.",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-06-03T09:10:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-06-03T09:10:00-05:00")));
        documentationService.submitDocumentation(caregiverMembership, record.getId(), OffsetDateTime.parse("2026-06-03T09:15:00-05:00"));

        ReviewWorkItem workItem = reviewWorkspaceService.createWorkItem(owner, new CreateReviewWorkItemCommand(
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                record.getId(),
                ReviewPriority.NORMAL,
                OffsetDateTime.parse("2026-06-03T10:00:00-05:00"),
                null,
                false,
                null,
                null));

        assertThatThrownBy(() -> reviewWorkspaceService.createWorkItem(owner, new CreateReviewWorkItemCommand(
                ReviewSourceType.VISIT_DOCUMENTATION_RECORD,
                record.getId(),
                ReviewPriority.NORMAL,
                OffsetDateTime.parse("2026-06-03T10:05:00-05:00"),
                null,
                false,
                null,
                null)))
                .isInstanceOf(ReviewConflictException.class);

        assertThatThrownBy(() -> reviewWorkspaceService.assignWorkItem(foreignBranchReviewer, workItem.getId(), new AssignReviewWorkCommand(
                qaReviewer.getId(),
                OffsetDateTime.parse("2026-06-03T10:10:00-05:00"),
                "Attempt")))
                .isInstanceOf(UnauthorizedReviewActorException.class);
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Epic10", role.name(), email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
