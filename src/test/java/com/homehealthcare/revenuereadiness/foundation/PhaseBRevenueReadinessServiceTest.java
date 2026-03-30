package com.homehealthcare.revenuereadiness.foundation;

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
import com.homehealthcare.evv.application.EvvVerificationService.RecordClockEventCommand;
import com.homehealthcare.evv.application.EvvVerificationService.RecordSignatureStatusCommand;
import com.homehealthcare.evv.domain.EvvClockEventType;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.EndVisitExecutionCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.foundation.MobileSyncDisposition;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorization;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationRepository;
import com.homehealthcare.patientauthorization.domain.PatientEpisodeAuthorizationStatus;
import com.homehealthcare.patientpayer.domain.PatientPayerLink;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkRepository;
import com.homehealthcare.patientpayer.domain.PatientPayerLinkStatus;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessConflictException;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.GenerateExportCommand;
import com.homehealthcare.revenuereadiness.application.RevenueReadinessService.RefreshVisitProjectionCommand;
import com.homehealthcare.revenuereadiness.application.UnauthorizedRevenueReadinessActorException;
import com.homehealthcare.revenuereadiness.domain.InvoiceExportRow;
import com.homehealthcare.revenuereadiness.domain.PayrollExportRow;
import com.homehealthcare.revenuereadiness.domain.RevenueExceptionFlagRepository;
import com.homehealthcare.revenuereadiness.domain.RevenueReadinessProjection;
import com.homehealthcare.revenuereadiness.foundation.RevenueReadinessStatus;
import com.homehealthcare.revenuereadiness.foundation.RevenueUsagePosture;
import com.homehealthcare.revenuereadiness.foundation.RevenueValidationOutcome;
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
class PhaseBRevenueReadinessServiceTest {

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
    private PatientPayerLinkRepository patientPayerLinkRepository;
    @Autowired
    private PatientEpisodeAuthorizationRepository patientEpisodeAuthorizationRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private DocumentationService documentationService;
    @Autowired
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private EvvVerificationService evvVerificationService;
    @Autowired
    private RevenueReadinessService revenueReadinessService;
    @Autowired
    private RevenueExceptionFlagRepository revenueExceptionFlagRepository;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void revenuePhaseBBuildsReadinessFlagsAuthorizationUsageAndExportRows() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Revenue Ready Home Health",
                "revenue-ready",
                "America/Chicago",
                "ops@revenue-ready.example"));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Billing", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@revenue-ready.example");
        AgencyMembership biller = persistMembership(agency, AgencyRole.BILLING_BACK_OFFICE, "billing@revenue-ready.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@revenue-ready.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(biller, branch));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caregiverMembership, branch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-REV-1",
                "Iris",
                null,
                "Revenue",
                null,
                LocalDate.of(1950, 4, 9),
                "F",
                "312-555-0100",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care service", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Personal Care", "PC-R", "Routine personal care", 60, true, 1));

        PatientPayerLink payerLink = patientPayerLinkRepository.saveAndFlush(PatientPayerLink.create(
                patient,
                "Prime Payer",
                "PRIME",
                "POL-1",
                null,
                LocalDate.of(2026, 1, 1),
                null,
                true,
                PatientPayerLinkStatus.ACTIVE,
                null));
        PatientEpisodeAuthorization authorization = patientEpisodeAuthorizationRepository.saveAndFlush(PatientEpisodeAuthorization.create(
                patient,
                payerLink,
                serviceLine,
                "AUTH-REV-1",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                1,
                0,
                PatientEpisodeAuthorizationStatus.ACTIVE,
                null));

        var templateAggregate = documentationService.createTemplate(owner, new ManageTemplateCommand(
                "Revenue Visit Note",
                "REV-NOTE",
                DocumentationTemplateType.VISIT_NOTE,
                "{\"version\":1}",
                1,
                serviceLine.getId(),
                visitType.getId(),
                branch.getId(),
                "Revenue note",
                Set.of(AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER, AgencyRole.BRANCH_ADMIN),
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

        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-07-08T09:00:00-05:00"),
                OffsetDateTime.parse("2026-07-08T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Revenue visit"));
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                branch,
                "CG-REV-1",
                "Casey Revenue",
                "Full Time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(caregiverProfile.getId(), branch.getId(), "board", "assign"));

        VisitDocumentationRecord documentationRecord = documentationService.createDocumentationRecord(
                caregiverMembership,
                new CreateDocumentationRecordCommand(visit.getId(), templateAggregate.template().getId(), OffsetDateTime.parse("2026-07-08T09:05:00-05:00")))
                .record();
        documentationService.saveDraft(caregiverMembership, documentationRecord.getId(), new SaveDocumentationDraftCommand(
                List.of(new FieldResponseCommand(
                        templateAggregate.fields().get(0).getId(),
                        "{\"text\":\"Visit completed with full care delivered.\"}",
                        "Visit completed with full care delivered.",
                        null,
                        DocumentationResponseState.COMPLETED,
                        OffsetDateTime.parse("2026-07-08T09:20:00-05:00"))),
                List.of(),
                OffsetDateTime.parse("2026-07-08T09:20:00-05:00")));
        documentationService.submitDocumentation(caregiverMembership, documentationRecord.getId(), OffsetDateTime.parse("2026-07-08T09:30:00-05:00"));

        var executionSession = mobileExecutionService.startVisitExecution(caregiverMembership, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-07-08T09:01:00-05:00"),
                BigDecimal.valueOf(41.881100),
                BigDecimal.valueOf(-87.623900),
                "gps",
                MobileSyncDisposition.ACCEPTED));
        mobileExecutionService.endVisitExecution(caregiverMembership, executionSession.getId(), new EndVisitExecutionCommand(
                OffsetDateTime.parse("2026-07-08T10:02:00-05:00"),
                BigDecimal.valueOf(41.881100),
                BigDecimal.valueOf(-87.623900),
                "gps",
                MobileSyncDisposition.ACCEPTED));

        var verificationSession = evvVerificationService.openVerificationSession(
                caregiverMembership,
                visit.getId(),
                executionSession.getId(),
                OffsetDateTime.parse("2026-07-08T09:01:00-05:00"));
        evvVerificationService.recordClockEvent(caregiverMembership, verificationSession.getId(), new RecordClockEventCommand(
                EvvClockEventType.CLOCK_IN,
                OffsetDateTime.parse("2026-07-08T09:01:00-05:00"),
                BigDecimal.valueOf(41.881100),
                BigDecimal.valueOf(-87.623900),
                "America/Chicago",
                "gps",
                null,
                null,
                null,
                null,
                null,
                null));
        evvVerificationService.recordClockEvent(caregiverMembership, verificationSession.getId(), new RecordClockEventCommand(
                EvvClockEventType.CLOCK_OUT,
                OffsetDateTime.parse("2026-07-08T10:02:00-05:00"),
                BigDecimal.valueOf(41.881100),
                BigDecimal.valueOf(-87.623900),
                "America/Chicago",
                "gps",
                null,
                null,
                null,
                null,
                null,
                null));
        evvVerificationService.recordSignatureStatus(caregiverMembership, verificationSession.getId(), new RecordSignatureStatusCommand(
                null,
                SignatureSignerRole.CAREGIVER,
                SignatureVerificationStatus.PRESENT,
                OffsetDateTime.parse("2026-07-08T10:03:00-05:00")));

        RevenueReadinessProjection blockedProjection = revenueReadinessService.recalculateRevenueReadiness(
                biller,
                new RefreshVisitProjectionCommand(visit.getId(), OffsetDateTime.parse("2026-07-08T10:05:00-05:00")));
        assertThat(blockedProjection.getReadinessStatus()).isEqualTo(RevenueReadinessStatus.BLOCKED);
        assertThat(revenueExceptionFlagRepository.findAllByAgency_IdOrderByDetectedAtDesc(agency.getId()))
                .anyMatch(flag -> flag.getExceptionType().name().equals("MISSING_SIGNATURE") && flag.isActive());

        assertThatThrownBy(() -> revenueReadinessService.generateInvoiceExportRow(
                biller,
                new GenerateExportCommand(visit.getId(), false, OffsetDateTime.parse("2026-07-08T10:06:00-05:00"))))
                .isInstanceOf(RevenueReadinessConflictException.class);

        evvVerificationService.recordSignatureStatus(caregiverMembership, verificationSession.getId(), new RecordSignatureStatusCommand(
                null,
                SignatureSignerRole.PATIENT,
                SignatureVerificationStatus.PRESENT,
                OffsetDateTime.parse("2026-07-08T10:07:00-05:00")));

        RevenueReadinessProjection warningProjection = revenueReadinessService.recalculateRevenueReadiness(
                biller,
                new RefreshVisitProjectionCommand(visit.getId(), OffsetDateTime.parse("2026-07-08T10:08:00-05:00")));
        assertThat(warningProjection.getCompletionValidation().getOutcome())
                .as(warningProjection.getCompletionValidation().getReasonCode())
                .isEqualTo(RevenueValidationOutcome.PASS);
        assertThat(warningProjection.getSignatureValidation().getOutcome())
                .as(warningProjection.getSignatureValidation().getReasonCode())
                .isEqualTo(RevenueValidationOutcome.WARNING);
        assertThat(warningProjection.getReadinessStatus()).isEqualTo(RevenueReadinessStatus.WARNING);
        assertThat(warningProjection.getAuthorizationUsageSnapshot()).isNotNull();
        assertThat(warningProjection.getAuthorizationUsageSnapshot().getUsagePosture()).isEqualTo(RevenueUsagePosture.NEAR_LIMIT);

        PayrollExportRow payrollRow = revenueReadinessService.generatePayrollExportRow(
                biller,
                new GenerateExportCommand(visit.getId(), false, OffsetDateTime.parse("2026-07-08T10:09:00-05:00")));
        InvoiceExportRow invoiceRow = revenueReadinessService.generateInvoiceExportRow(
                biller,
                new GenerateExportCommand(visit.getId(), false, OffsetDateTime.parse("2026-07-08T10:10:00-05:00")));

        assertThat(payrollRow.getDurationMinutes()).isEqualTo(61);
        assertThat(invoiceRow.getBillableUnits()).isEqualTo(1);
        assertThat(invoiceRow.getPayerName()).isEqualTo("Prime Payer");
        assertThat(invoiceRow.getAuthorizationNumber()).isEqualTo("AUTH-REV-1");
        assertThat(auditEventRepository.findAll()).anySatisfy(event ->
                assertThat(event.getActionType()).isEqualTo("REVENUE_EXPORT_GENERATED"));
    }

    @Test
    void revenuePhaseBEnforcesBranchAwareAuthorization() {
        Agency agency = agencyRepository.saveAndFlush(Agency.create(
                "Scoped Revenue Health",
                "scoped-revenue",
                "America/Chicago",
                "ops@scoped-revenue.example"));
        Branch allowedBranch = branchRepository.saveAndFlush(Branch.create(agency, "Allowed", "ALW", "Chicago", "America/Chicago"));
        Branch hiddenBranch = branchRepository.saveAndFlush(Branch.create(agency, "Hidden", "HID", "Aurora", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@scoped-revenue.example");
        AgencyMembership biller = persistMembership(agency, AgencyRole.BILLING_BACK_OFFICE, "billing@scoped-revenue.example");
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(biller, allowedBranch));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-REV-2",
                "Mila",
                null,
                "Scoped",
                null,
                LocalDate.of(1957, 2, 11),
                "F",
                "312-555-0177",
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "SN", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "SN Visit", "SN-V", "SN", 60, true, 1));

        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                hiddenBranch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-07-09T09:00:00-05:00"),
                OffsetDateTime.parse("2026-07-09T10:00:00-05:00"),
                "America/Chicago",
                "normal",
                "manual",
                "Hidden branch revenue visit"));

        assertThatThrownBy(() -> revenueReadinessService.recalculateRevenueReadiness(
                biller,
                new RefreshVisitProjectionCommand(visit.getId(), OffsetDateTime.parse("2026-07-09T10:00:00-05:00"))))
                .isInstanceOf(UnauthorizedRevenueReadinessActorException.class);
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = User.invite("Test", "User", email, null);
        user.activate();
        user = userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
