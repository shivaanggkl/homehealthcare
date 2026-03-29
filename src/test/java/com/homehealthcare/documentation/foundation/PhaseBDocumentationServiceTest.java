package com.homehealthcare.documentation.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.documentation.application.DocumentationConflictException;
import com.homehealthcare.documentation.application.DocumentationService;
import com.homehealthcare.documentation.application.DocumentationValidationException;
import com.homehealthcare.documentation.application.DocumentationService.CreateDocumentationRecordCommand;
import com.homehealthcare.documentation.application.DocumentationService.FieldDefinitionCommand;
import com.homehealthcare.documentation.application.DocumentationService.FieldResponseCommand;
import com.homehealthcare.documentation.application.DocumentationService.ManageTaskLibraryItemCommand;
import com.homehealthcare.documentation.application.DocumentationService.ManageTemplateCommand;
import com.homehealthcare.documentation.application.DocumentationService.SaveDocumentationDraftCommand;
import com.homehealthcare.documentation.application.DocumentationService.SectionCommand;
import com.homehealthcare.documentation.application.DocumentationService.TaskResponseCommand;
import com.homehealthcare.documentation.application.DocumentationService.TemplateTaskCommand;
import com.homehealthcare.documentation.application.UnauthorizedDocumentationActorException;
import com.homehealthcare.documentation.domain.DocumentationAttachmentLink;
import com.homehealthcare.documentation.domain.DocumentationFieldResponse;
import com.homehealthcare.documentation.domain.VisitDocumentationRecord;
import com.homehealthcare.documentationtemplate.domain.DocumentationTemplateType;
import com.homehealthcare.evv.application.EvvVerificationService;
import com.homehealthcare.evv.application.EvvVerificationService.RecordClockEventCommand;
import com.homehealthcare.evv.application.EvvVerificationService.RecordSignatureStatusCommand;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileFieldSupportService;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import com.homehealthcare.patientattachment.domain.PatientAttachmentRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
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
class PhaseBDocumentationServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ServiceLineRepository serviceLineRepository;
    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private DocumentationService documentationService;
    @Autowired
    private PatientAttachmentRepository patientAttachmentRepository;
    @Autowired
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private MobileFieldSupportService mobileFieldSupportService;
    @Autowired
    private EvvVerificationService evvVerificationService;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void documentationPhaseBSupportsTemplatesTasksDraftValidationAttachmentsAndPrintableSummary() {
        Agency agency = persistAgency("north-star-docs");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-800",
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
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine visit instructions", 60, true, 1));
        TaskTemplate taskLibrary = documentationService.createTaskLibraryItem(owner, new ManageTaskLibraryItemCommand(
                serviceLine.getId(),
                visitType.getId(),
                "Vitals",
                "VITALS",
                "Collect vitals",
                TaskTemplateCategory.CLINICAL,
                1,
                1,
                "Complete before departure",
                true));

        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Skilled Nursing Routine Note",
                "SN-RV",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                branch.getId(),
                "Complete before submitting.",
                Set.of(AgencyRole.CAREGIVER, AgencyRole.BRANCH_ADMIN, AgencyRole.QA_CLINICAL_REVIEWER),
                true,
                List.of(
                        new SectionCommand("overview", "Overview", "Visit overview", 1),
                        new SectionCommand("clinical", "Clinical", "Clinical observations", 2)),
                List.of(
                        new FieldDefinitionCommand("overview", "narrative", "Visit Narrative", DocumentationFieldType.LONG_TEXT, true, 1, null, null,
                                Set.of(AgencyRole.CAREGIVER, AgencyRole.BRANCH_ADMIN),
                                Set.of(AgencyRole.CAREGIVER, AgencyRole.BRANCH_ADMIN)),
                        new FieldDefinitionCommand("clinical", "bp_reading", "Blood Pressure", DocumentationFieldType.TEXT, true, 2, null, null,
                                Set.of(AgencyRole.CAREGIVER, AgencyRole.BRANCH_ADMIN),
                                Set.of(AgencyRole.CAREGIVER, AgencyRole.BRANCH_ADMIN))),
                List.of(new TemplateTaskCommand("clinical", taskLibrary.getId(), null, null, null, 1))));

        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-05-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-05-10T10:00:00-05:00"),
                "America/Chicago",
                "urgent",
                "manual",
                "Knock before entering"));

        // leverage assignment support already in Epic 5 so caregiver can execute mobile/EVV flow
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiver, branch, "CG-008", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        var caregiverAssignment = schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                branch.getId(),
                "board",
                "assigned"));
        assertThat(caregiverAssignment.getId()).isNotNull();

        VisitDocumentationRecord documentationRecord = documentationService.createDocumentationRecord(
                caregiver,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-05-10T09:05:00-05:00")))
                .record();

        assertThatThrownBy(() -> documentationService.submitDocumentation(caregiver, documentationRecord.getId(), OffsetDateTime.parse("2026-05-10T09:10:00-05:00")))
                .isInstanceOf(DocumentationValidationException.class);

        DocumentationFieldResponse narrativeField = documentationService.saveDraft(
                caregiver,
                documentationRecord.getId(),
                new SaveDocumentationDraftCommand(
                        List.of(
                                new FieldResponseCommand(
                                        templateAggregate.fields().get(0).getId(),
                                        "{\"text\":\"Patient tolerated visit well.\"}",
                                        "Patient tolerated visit well.",
                                        null,
                                        DocumentationResponseState.COMPLETED,
                                        OffsetDateTime.parse("2026-05-10T09:11:00-05:00")),
                                new FieldResponseCommand(
                                        templateAggregate.fields().get(1).getId(),
                                        "{\"text\":\"120/78\"}",
                                        "120/78",
                                        null,
                                        DocumentationResponseState.COMPLETED,
                                        OffsetDateTime.parse("2026-05-10T09:12:00-05:00"))),
                        List.of(new TaskResponseCommand(
                                templateAggregate.tasks().get(0).getId(),
                                DocumentationResponseState.COMPLETED,
                                "Vitals obtained",
                                OffsetDateTime.parse("2026-05-10T09:13:00-05:00"))),
                        OffsetDateTime.parse("2026-05-10T09:13:00-05:00")))
                .fieldResponses()
                .get(0);

        assertThat(narrativeField.getCompletionState()).isEqualTo(DocumentationResponseState.COMPLETED);

        PatientAttachment patientAttachment = patientAttachmentRepository.saveAndFlush(PatientAttachment.create(
                patient,
                owner,
                "care-plan.pdf",
                "application/pdf",
                1024,
                "patient/care-plan.pdf",
                "CARE_PLAN",
                "Existing care plan"));

        // mobile execution + signature-backed artifact for attachment/signature requirement
        var session = mobileExecutionService.startVisitExecution(
                caregiver,
                visit.getId(),
                new StartVisitExecutionCommand(
                        OffsetDateTime.parse("2026-05-10T09:02:00-05:00"),
                        BigDecimal.valueOf(41.881000),
                        BigDecimal.valueOf(-87.623000),
                        "mobile_app",
                        null));
        MobileFieldArtifact signatureArtifact = mobileFieldSupportService.uploadArtifact(
                caregiver,
                session.getId(),
                new MobileFieldSupportService.UploadMobileArtifactCommand(
                        MobileFieldArtifactType.SIGNATURE,
                        "signature.png",
                        "image/png",
                        "signature-bytes".getBytes(),
                        "Patient signature"));
        var verificationSession = evvVerificationService.openVerificationSession(
                caregiver,
                visit.getId(),
                session.getId(),
                OffsetDateTime.parse("2026-05-10T09:02:00-05:00"));
        evvVerificationService.recordClockEvent(caregiver, verificationSession.getId(), new RecordClockEventCommand(
                com.homehealthcare.evv.domain.EvvClockEventType.CLOCK_IN,
                OffsetDateTime.parse("2026-05-10T09:02:00-05:00"),
                BigDecimal.valueOf(41.881000),
                BigDecimal.valueOf(-87.623000),
                "America/Chicago",
                "mobile_app",
                null,
                null,
                null,
                null,
                null,
                null));
        evvVerificationService.recordSignatureStatus(caregiver, verificationSession.getId(), new RecordSignatureStatusCommand(
                signatureArtifact.getId(),
                SignatureSignerRole.PATIENT,
                SignatureVerificationStatus.PRESENT,
                OffsetDateTime.parse("2026-05-10T09:20:00-05:00")));

        DocumentationAttachmentLink patientLink = documentationService.linkPatientAttachment(
                caregiver,
                documentationRecord.getId(),
                patientAttachment.getId(),
                "Care plan",
                "Care plan reference");
        DocumentationAttachmentLink artifactLink = documentationService.linkMobileArtifact(
                caregiver,
                documentationRecord.getId(),
                signatureArtifact.getId(),
                "Signature",
                "Captured during visit");

        var submitted = documentationService.submitDocumentation(caregiver, documentationRecord.getId(), OffsetDateTime.parse("2026-05-10T09:30:00-05:00"));
        var printable = documentationService.generatePrintableSummary(owner, documentationRecord.getId());

        assertThat(patientLink.getId()).isNotNull();
        assertThat(artifactLink.getId()).isNotNull();
        assertThat(submitted.record().getStatus()).isEqualTo(DocumentationRecordStatus.SUBMITTED);
        assertThat(printable.templateTitle()).isEqualTo("Skilled Nursing Routine Note");
        assertThat(printable.fields()).extracting(DocumentationService.PrintableField::displayValue)
                .contains("Patient tolerated visit well.", "120/78");
        assertThat(printable.tasks()).extracting(DocumentationService.PrintableTask::completionState)
                .contains("COMPLETED");
        assertThat(printable.attachments()).hasSize(2);

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());
        assertThat(events).extracting(AuditEvent::getActionType)
                .contains(
                        Epic8DocumentationAuditAction.TEMPLATE_CREATED.actionType(),
                        Epic8DocumentationAuditAction.TASK_LIBRARY_UPDATED.actionType(),
                        Epic8DocumentationAuditAction.DOCUMENTATION_DRAFT_SAVED.actionType(),
                        Epic8DocumentationAuditAction.ATTACHMENT_LINKED.actionType(),
                        Epic8DocumentationAuditAction.DOCUMENTATION_SUBMITTED.actionType(),
                        Epic8DocumentationAuditAction.PRINTABLE_SUMMARY_GENERATED.actionType());
    }

    @Test
    void documentationPhaseBRejectsWrongTemplateScopeAndUnauthorizedAmend() {
        Agency agency = persistAgency("north-star-docs-scope");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        Branch otherBranch = branchRepository.saveAndFlush(Branch.create(agency, "South Branch", "SB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner2@northstar.example");
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver2@northstar.example");
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency, "PAT-801", "Lia", null, "Patient", null, LocalDate.of(1960, 7, 10), "F", null, null, null, "en-US", null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Therapy", "TH", "Therapy", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Therapy Visit", "TV", "Therapy", 45, true, 1));
        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "South Branch Therapy Note",
                "SB-TV",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                otherBranch.getId(),
                null,
                Set.of(AgencyRole.CAREGIVER),
                false,
                List.of(new SectionCommand("overview", "Overview", null, 1)),
                List.of(new FieldDefinitionCommand("overview", "summary", "Summary", DocumentationFieldType.TEXT, true, 1, null, null, Set.of(AgencyRole.CAREGIVER), Set.of(AgencyRole.CAREGIVER))),
                List.of()));

        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-06-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-06-10T09:45:00-05:00"),
                "America/Chicago",
                "routine",
                "manual",
                null));

        assertThatThrownBy(() -> documentationService.createDocumentationRecord(
                caregiver,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.now())))
                .isInstanceOf(DocumentationConflictException.class)
                .hasMessageContaining("branch");

        assertThatThrownBy(() -> documentationService.amendDocumentation(
                caregiver,
                UUID.randomUUID(),
                new SaveDocumentationDraftCommand(List.of(), List.of(), OffsetDateTime.now())))
                .isInstanceOfAny(UnauthorizedDocumentationActorException.class, com.homehealthcare.documentation.application.DocumentationEntityNotFoundException.class);
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(Agency.create("North Star Home Care", slug, "America/Chicago", "ops@northstar.example"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Test", role.name(), email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
