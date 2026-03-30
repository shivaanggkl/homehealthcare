package com.homehealthcare.compliance.foundation;

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
import com.homehealthcare.compliance.application.ComplianceWorkspaceService;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.CreateChecklistDefinitionCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.CreateDocumentationRequirementCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.EvaluateDocumentationRequirementCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.RecalculateChecklistResultCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.RecalculateStatusProjectionCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.RecordConsentAcknowledgmentCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.SaveCertificationPeriodCommand;
import com.homehealthcare.compliance.application.ComplianceWorkspaceService.SaveRiskReminderCommand;
import com.homehealthcare.compliance.application.UnauthorizedComplianceActorException;
import com.homehealthcare.compliance.domain.ComplianceChecklistDefinition;
import com.homehealthcare.compliance.domain.ComplianceChecklistResult;
import com.homehealthcare.compliance.domain.ComplianceStatusProjection;
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
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
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
class PhaseBComplianceWorkspaceServiceTest {

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
    private PatientAttachmentRepository patientAttachmentRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private DocumentationService documentationService;
    @Autowired
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private EvvVerificationService evvVerificationService;
    @Autowired
    private ComplianceWorkspaceService complianceWorkspaceService;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void compliancePhaseBSupportsChecklistDocumentationAcknowledgmentCertificationReminderProjectionAndDashboard() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Compliance First Home Health",
                "compliance-first",
                "America/Chicago",
                "ops@compliance-first.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Compliance", "NC", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Compliance", "SC", "Aurora", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@compliance-first.example");
        AgencyMembership reviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "reviewer@compliance-first.example");
        AgencyMembership outOfScopeReviewer = persistMembership(agency, AgencyRole.QA_CLINICAL_REVIEWER, "other-reviewer@compliance-first.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@compliance-first.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(reviewer, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(outOfScopeReviewer, otherBranch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiverMembership, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-COMP-1",
                "Nora",
                null,
                "Careplan",
                null,
                LocalDate.of(1951, 8, 3),
                "F",
                "312-555-0199",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine SN", "SN-R", "Routine SN visit", 60, true, 1));

        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Compliance Visit Note",
                "COMP-NOTE",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                branch.getId(),
                "Compliance note",
                Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER, AgencyRole.BRANCH_ADMIN),
                false,
                List.of(new SectionCommand("overview", "Overview", "Overview", 1)),
                List.of(new FieldDefinitionCommand(
                        "overview",
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

        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-07-01T09:00:00-05:00"),
                OffsetDateTime.parse("2026-07-01T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Compliance visit"));
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                branch,
                "CG-COMP-1",
                "Casey Compliance",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", "assign"));

        VisitDocumentationRecord documentationRecord = documentationService.createDocumentationRecord(
                caregiverMembership,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-07-01T09:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiverMembership, documentationRecord.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(0).getId(),
                        "{\"text\":\"Patient remains stable with complete plan of care.\"}",
                        "Patient remains stable with complete plan of care.",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-07-01T09:15:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-07-01T09:15:00-05:00")));
        documentationService.submitDocumentation(caregiverMembership, documentationRecord.getId(), OffsetDateTime.parse("2026-07-01T09:30:00-05:00"));

        ComplianceChecklistDefinition checklistDefinition = complianceWorkspaceService.createChecklistDefinition(owner, new CreateChecklistDefinitionCommand(
                branch.getId(),
                serviceLine.getId(),
                "PLAN_COMPLETE",
                "Plan of care has required elements",
                "HIGH",
                5,
                true));
        ComplianceChecklistResult checklistResult = complianceWorkspaceService.recalculateChecklistResult(reviewer, new RecalculateChecklistResultCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                checklistDefinition.getId(),
                ComplianceChecklistResultStatus.PASS,
                "DOCUMENTATION_RECORD",
                documentationRecord.getId(),
                "All required care-plan fields are present.",
                "SYSTEM",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                OffsetDateTime.parse("2026-07-01T10:00:00-05:00")));

        var requirement = complianceWorkspaceService.createDocumentationRequirement(owner, new CreateDocumentationRequirementCommand(
                branch.getId(),
                serviceLine.getId(),
                "VISIT_NOTE",
                "SIGNED_ATTACHMENT_NOTE",
                "Visit note must be recent and include attachment plus signature",
                true,
                true,
                2,
                5,
                true,
                true,
                true));

        ComplianceChecklistResult failingRequirement = complianceWorkspaceService.evaluateDocumentationRequirement(reviewer, new EvaluateDocumentationRequirementCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                requirement.getId(),
                OffsetDateTime.parse("2026-07-01T09:00:00-05:00"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "SYSTEM",
                OffsetDateTime.parse("2026-07-01T10:05:00-05:00")));
        assertThat(failingRequirement.getResultStatus()).isEqualTo(ComplianceChecklistResultStatus.FAIL);
        assertThat(failingRequirement.getMissingReason()).isEqualTo("ATTACHMENT_EVIDENCE_MISSING");

        PatientAttachment attachment = patientAttachmentRepository.saveAndFlush(PatientAttachment.create(
                patient,
                caregiverMembership,
                "consent.pdf",
                "application/pdf",
                512,
                "patient/consent.pdf",
                "consent",
                "Signed consent"));
        documentationService.linkPatientAttachment(caregiverMembership, documentationRecord.getId(), attachment.getId(), "Consent", "Support");

        var executionSession = mobileExecutionService.startVisitExecution(caregiverMembership, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-07-01T09:01:00-05:00"),
                BigDecimal.valueOf(41.881),
                BigDecimal.valueOf(-87.623),
                "mobile_app",
                null));
        var verificationSession = evvVerificationService.openVerificationSession(
                caregiverMembership,
                visit.getId(),
                executionSession.getId(),
                OffsetDateTime.parse("2026-07-01T09:02:00-05:00"));
        evvVerificationService.recordSignatureStatus(caregiverMembership, verificationSession.getId(), new RecordSignatureStatusCommand(
                null,
                SignatureSignerRole.PATIENT,
                SignatureVerificationStatus.PRESENT,
                OffsetDateTime.parse("2026-07-01T09:25:00-05:00")));

        ComplianceChecklistResult satisfiedRequirement = complianceWorkspaceService.evaluateDocumentationRequirement(reviewer, new EvaluateDocumentationRequirementCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                requirement.getId(),
                OffsetDateTime.parse("2026-07-01T09:00:00-05:00"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "SYSTEM",
                OffsetDateTime.parse("2026-07-01T10:10:00-05:00")));
        assertThat(satisfiedRequirement.getResultStatus()).isEqualTo(ComplianceChecklistResultStatus.PASS);
        assertThat(satisfiedRequirement.getSatisfiedByRecordId()).isEqualTo(documentationRecord.getId());

        ComplianceChecklistResult staleRequirement = complianceWorkspaceService.evaluateDocumentationRequirement(reviewer, new EvaluateDocumentationRequirementCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                requirement.getId(),
                OffsetDateTime.parse("2026-07-01T09:00:00-05:00"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                "SYSTEM",
                OffsetDateTime.parse("2026-07-15T07:00:00-05:00")));
        assertThat(staleRequirement.getResultStatus()).isEqualTo(ComplianceChecklistResultStatus.WARNING);
        assertThat(staleRequirement.getMissingReason()).isEqualTo("DOCUMENTATION_STALE");

        var acknowledgment = complianceWorkspaceService.recordAcknowledgment(reviewer, new RecordConsentAcknowledgmentCommand(
                patient.getId(),
                branch.getId(),
                "RIGHT_TO_CARE",
                OffsetDateTime.parse("2026-07-01T11:00:00-05:00"),
                OffsetDateTime.parse("2026-09-01T00:00:00-05:00"),
                caregiverMembership.getId(),
                "PATIENT_SIGNATURE",
                "PATIENT_ATTACHMENT",
                attachment.getId()));
        assertThat(acknowledgment.getStatus()).isEqualTo(ConsentAcknowledgmentStatus.ACTIVE);

        var certification = complianceWorkspaceService.saveCertificationPeriod(reviewer, new SaveCertificationPeriodCommand(
                null,
                patient.getId(),
                branch.getId(),
                null,
                "MEDICARE",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31),
                false,
                "MANUAL"));
        assertThat(certification.projectStatus(LocalDate.of(2026, 7, 15), 10)).isEqualTo(CertificationPeriodStatus.CURRENT);

        var reminder = complianceWorkspaceService.saveRiskReminder(reviewer, new SaveRiskReminderCommand(
                null,
                patient.getId(),
                branch.getId(),
                visit.getId(),
                documentationRecord.getId(),
                "FALL_RISK",
                "HIGH",
                "Monitor for elevated fall risk.",
                OffsetDateTime.parse("2026-07-01T10:15:00-05:00"),
                OffsetDateTime.parse("2026-08-15T00:00:00-05:00"),
                "DOCUMENTATION_RECORD",
                documentationRecord.getId()));
        assertThat(reminder.getStatus()).isEqualTo(PatientRiskReminderStatus.ACTIVE);

        ComplianceStatusProjection warningProjection = complianceWorkspaceService.recalculateStatusProjection(reviewer, new RecalculateStatusProjectionCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                Set.of("RIGHT_TO_CARE"),
                10,
                OffsetDateTime.parse("2026-07-15T08:00:00-05:00")));
        assertThat(warningProjection.getChecklistPassCount()).isEqualTo(1);
        assertThat(warningProjection.getDocumentationWarningCount()).isEqualTo(1);
        assertThat(warningProjection.getActiveRiskReminderCount()).isEqualTo(1);
        assertThat(warningProjection.getReadinessStatus()).isEqualTo(ComplianceReadinessStatus.WARNING);

        complianceWorkspaceService.resolveRiskReminder(reviewer, reminder.getId(), OffsetDateTime.parse("2026-07-15T09:00:00-05:00"));
        ComplianceStatusProjection readyProjection = complianceWorkspaceService.recalculateStatusProjection(reviewer, new RecalculateStatusProjectionCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                Set.of("RIGHT_TO_CARE"),
                10,
                OffsetDateTime.parse("2026-07-03T08:00:00-05:00")));
        assertThat(readyProjection.getDocumentationSatisfiedCount()).isEqualTo(1);
        assertThat(readyProjection.getDocumentationWarningCount()).isZero();
        assertThat(readyProjection.getActiveRiskReminderCount()).isZero();
        assertThat(readyProjection.getReadinessStatus()).isEqualTo(ComplianceReadinessStatus.READY);

        var workspace = complianceWorkspaceService.getPatientWorkspace(reviewer, patient.getId(), branch.getId(), serviceLine.getId());
        assertThat(workspace.projection()).isNotNull();
        assertThat(workspace.results()).extracting(ComplianceChecklistResult::getEvaluationCategory)
                .contains(ComplianceEvaluationCategory.CHECKLIST_ITEM, ComplianceEvaluationCategory.REQUIRED_DOCUMENTATION);

        var dashboard = complianceWorkspaceService.aggregateDashboard(reviewer, branch.getId(), null);
        assertThat(dashboard).singleElement().satisfies(item -> {
            assertThat(item.branchId()).isEqualTo(branch.getId());
            assertThat(item.readyCount()).isEqualTo(1);
            assertThat(item.totalPatients()).isEqualTo(1);
        });

        assertThatThrownBy(() -> complianceWorkspaceService.recalculateStatusProjection(
                outOfScopeReviewer,
                new RecalculateStatusProjectionCommand(
                        patient.getId(),
                        branch.getId(),
                        serviceLine.getId(),
                        Set.of("RIGHT_TO_CARE"),
                        10,
                        OffsetDateTime.parse("2026-07-03T08:00:00-05:00"))))
                .isInstanceOf(UnauthorizedComplianceActorException.class);

        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(reviewer.getId()))
                .extracting(event -> event.getActionType())
                .contains(
                        Epic11ComplianceAuditAction.CHECKLIST_RESULT_RECALCULATED.actionType(),
                        Epic11ComplianceAuditAction.ACKNOWLEDGMENT_RECORDED.actionType(),
                        Epic11ComplianceAuditAction.CERTIFICATION_PERIOD_SAVED.actionType(),
                        Epic11ComplianceAuditAction.RISK_REMINDER_SAVED.actionType(),
                        Epic11ComplianceAuditAction.RISK_REMINDER_RESOLVED.actionType(),
                        Epic11ComplianceAuditAction.STATUS_PROJECTION_RECALCULATED.actionType());

        assertThat(checklistResult.getResultStatus()).isEqualTo(ComplianceChecklistResultStatus.PASS);
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite(role.name(), "User", email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
