package com.homehealthcare.evv.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.evv.application.EvvConflictException;
import com.homehealthcare.evv.application.EvvVerificationService;
import com.homehealthcare.evv.application.EvvVerificationService.CreateEscalationCommand;
import com.homehealthcare.evv.application.EvvVerificationService.LogVisitExceptionCommand;
import com.homehealthcare.evv.application.EvvVerificationService.NotifySupervisorCommand;
import com.homehealthcare.evv.application.EvvVerificationService.RecordClockEventCommand;
import com.homehealthcare.evv.application.EvvVerificationService.RecordSignatureStatusCommand;
import com.homehealthcare.evv.application.EvvVerificationService.ReportMissedVisitCommand;
import com.homehealthcare.evv.application.UnauthorizedEvvActorException;
import com.homehealthcare.evv.domain.EscalationRequest;
import com.homehealthcare.evv.domain.EvvClockEvent;
import com.homehealthcare.evv.domain.EvvClockEventType;
import com.homehealthcare.evv.domain.EvvVerificationSession;
import com.homehealthcare.evv.domain.GeofenceToleranceRule;
import com.homehealthcare.evv.domain.GeofenceToleranceRuleRepository;
import com.homehealthcare.evv.domain.MissedVisitRecord;
import com.homehealthcare.evv.domain.SignatureSignerRole;
import com.homehealthcare.evv.domain.SignatureVerificationLink;
import com.homehealthcare.evv.domain.SignatureVerificationStatus;
import com.homehealthcare.evv.domain.SupervisorNotificationEvent;
import com.homehealthcare.evv.domain.VisitExceptionRecord;
import com.homehealthcare.evv.domain.VisitExceptionSeverity;
import com.homehealthcare.evv.domain.VisitExceptionType;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.domain.MobileFieldArtifact;
import com.homehealthcare.mobile.domain.MobileFieldArtifactRepository;
import com.homehealthcare.mobile.domain.MobileFieldArtifactType;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
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
class PhaseBEvvVerificationServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PatientAddressRepository patientAddressRepository;
    @Autowired
    private ServiceLineRepository serviceLineRepository;
    @Autowired
    private VisitTypeRepository visitTypeRepository;
    @Autowired
    private SchedulingRecordService schedulingRecordService;
    @Autowired
    private MobileExecutionService mobileExecutionService;
    @Autowired
    private MobileFieldArtifactRepository mobileFieldArtifactRepository;
    @Autowired
    private GeofenceToleranceRuleRepository geofenceToleranceRuleRepository;
    @Autowired
    private EvvVerificationService evvVerificationService;
    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void evvVerificationServiceSupportsCorePhaseBWorkflowWithAudit() {
        Agency agency = persistAgency("north-star-evv");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        AgencyMembership branchAdmin = persistMembership(agency, AgencyRole.BRANCH_ADMIN, "admin@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-EVV-1", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency, "PAT-EVV-1", "Ava", null, "Patient", null, LocalDate.of(1950, 1, 1), "F", "312-555-0100", null, null, "en-US", null));
        patientAddressRepository.saveAndFlush(PatientAddress.create(
                patient, "123 Main St", null, "Chicago", "IL", "60601", "USA",
                BigDecimal.valueOf(41.881000), BigDecimal.valueOf(-87.623000), "GEOCODED", "America/Chicago", null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "SN", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine", 60, true, 1));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId(),
                OffsetDateTime.parse("2026-04-15T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-15T10:00:00-05:00"),
                "America/Chicago", "urgent", "manual", "Knock first"));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(), branch.getId(), "board", "assigned"));
        var executionSession = mobileExecutionService.startVisitExecution(caregiverMembership, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-04-15T09:01:00-05:00"),
                BigDecimal.valueOf(41.881001),
                BigDecimal.valueOf(-87.623001),
                "mobile_app",
                null));
        geofenceToleranceRuleRepository.saveAndFlush(GeofenceToleranceRule.createBranchOverride(branch, "Branch Rule", 300, 100, false));

        EvvVerificationSession verificationSession = evvVerificationService.openVerificationSession(
                caregiverMembership,
                visit.getId(),
                executionSession.getId(),
                OffsetDateTime.parse("2026-04-15T09:01:30-05:00"));

        EvvClockEvent clockIn = evvVerificationService.recordClockEvent(caregiverMembership, verificationSession.getId(), new RecordClockEventCommand(
                EvvClockEventType.CLOCK_IN,
                OffsetDateTime.parse("2026-04-15T09:01:00-05:00"),
                BigDecimal.valueOf(41.881001),
                BigDecimal.valueOf(-87.623001),
                "America/Chicago",
                "mobile_app",
                "IOS",
                "1.0.0",
                "PHONE",
                -300,
                "ua-hash",
                "session-hash"));
        EvvClockEvent clockOut = evvVerificationService.recordClockEvent(caregiverMembership, verificationSession.getId(), new RecordClockEventCommand(
                EvvClockEventType.CLOCK_OUT,
                OffsetDateTime.parse("2026-04-15T10:00:00-05:00"),
                BigDecimal.valueOf(41.881002),
                BigDecimal.valueOf(-87.623002),
                "America/Chicago",
                "mobile_app",
                null,
                null,
                null,
                null,
                null,
                null));
        MobileFieldArtifact signatureArtifact = mobileFieldArtifactRepository.saveAndFlush(MobileFieldArtifact.create(
                executionSession,
                visit,
                caregiverProfile,
                patient,
                branch,
                MobileFieldArtifactType.SIGNATURE,
                "signature.png",
                "image/png",
                1024,
                "mobile/signature.png",
                "Captured signature"));
        SignatureVerificationLink signature = evvVerificationService.recordSignatureStatus(caregiverMembership, verificationSession.getId(), new RecordSignatureStatusCommand(
                signatureArtifact.getId(),
                SignatureSignerRole.PATIENT,
                SignatureVerificationStatus.PRESENT,
                OffsetDateTime.parse("2026-04-15T10:01:00-05:00")));
        VisitExceptionRecord exceptionRecord = evvVerificationService.logVisitException(caregiverMembership, verificationSession.getId(), new LogVisitExceptionCommand(
                VisitExceptionType.LATE_START,
                VisitExceptionSeverity.MEDIUM,
                "TRAFFIC_DELAY",
                "Traffic caused a delayed arrival."));
        SupervisorNotificationEvent notificationEvent = evvVerificationService.notifySupervisorForMissedVisit(branchAdmin, evvVerificationService.reportMissedVisit(
                caregiverMembership,
                visit.getId(),
                new ReportMissedVisitCommand("PATIENT_REFUSED", "Patient refused the alternate follow-up visit.", OffsetDateTime.parse("2026-04-15T11:00:00-05:00"))).getId(), new NotifySupervisorCommand(
                branchAdmin.getId(),
                "IN_APP",
                "Needs supervisor review",
                OffsetDateTime.parse("2026-04-15T11:05:00-05:00")));
        EscalationRequest escalationRequest = evvVerificationService.createEscalationForException(branchAdmin, exceptionRecord.getId(), new CreateEscalationCommand(
                "QA_CLINICAL_REVIEWER",
                VisitExceptionSeverity.HIGH,
                "Create follow-up review task",
                OffsetDateTime.parse("2026-04-16T09:00:00-05:00")));
        EvvComplianceProjection projection = evvVerificationService.getComplianceProjection(caregiverMembership, verificationSession.getId());

        assertThat(clockIn.getVerificationStatus()).isEqualTo(EvvVerificationStatus.PENDING_VERIFICATION);
        assertThat(clockOut.getVerificationStatus()).isEqualTo(EvvVerificationStatus.PENDING_VERIFICATION);
        assertThat(signature.getVerificationStatus()).isEqualTo(SignatureVerificationStatus.PRESENT);
        assertThat(notificationEvent.getRecipientMembership().getId()).isEqualTo(branchAdmin.getId());
        assertThat(escalationRequest.getTargetRoleKey()).isEqualTo("QA_CLINICAL_REVIEWER");
        assertThat(projection.missedVisitReported()).isTrue();
        assertThat(projection.overallOutcome()).isEqualTo(EvvComplianceOutcome.MISSED_VISIT);
        assertThat(auditEventRepository.findAll()).extracting("actionType")
                .contains(
                        Epic7EvvAuditAction.CLOCK_IN_RECORDED.actionType(),
                        Epic7EvvAuditAction.CLOCK_OUT_RECORDED.actionType(),
                        Epic7EvvAuditAction.GEOFENCE_EVALUATED.actionType(),
                        Epic7EvvAuditAction.SIGNATURE_STATUS_RECORDED.actionType(),
                        Epic7EvvAuditAction.EXCEPTION_LOGGED.actionType(),
                        Epic7EvvAuditAction.MISSED_VISIT_REPORTED.actionType(),
                        Epic7EvvAuditAction.SUPERVISOR_NOTIFIED.actionType(),
                        Epic7EvvAuditAction.ESCALATION_CREATED.actionType());
    }

    @Test
    void evvVerificationServiceBlocksDuplicateClockEventsAndUnauthorizedActors() {
        Agency agency = persistAgency("north-star-evv-conflict");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner2@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver2@northstar.example");
        AgencyMembership otherCaregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver3@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-EVV-2", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        CaregiverProfile otherCaregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                otherCaregiverMembership, branch, "CG-EVV-3", "Other Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency, "PAT-EVV-2", "Ava", null, "Patient", null, LocalDate.of(1950, 1, 1), "F", "312-555-0100", null, null, "en-US", null));
        patientAddressRepository.saveAndFlush(PatientAddress.create(
                patient, "123 Main St", null, "Chicago", "IL", "60601", "USA",
                BigDecimal.valueOf(41.881000), BigDecimal.valueOf(-87.623000), "GEOCODED", "America/Chicago", null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN2", "SN", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV2", "Routine", 60, true, 1));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(), branch.getId(), serviceLine.getId(), visitType.getId(),
                OffsetDateTime.parse("2026-04-15T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-15T10:00:00-05:00"),
                "America/Chicago", "urgent", "manual", "Knock first"));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(), branch.getId(), "board", "assigned"));
        var executionSession = mobileExecutionService.startVisitExecution(caregiverMembership, visit.getId(), new StartVisitExecutionCommand(
                OffsetDateTime.parse("2026-04-15T09:01:00-05:00"),
                BigDecimal.valueOf(41.881001),
                BigDecimal.valueOf(-87.623001),
                "mobile_app",
                null));

        EvvVerificationSession verificationSession = evvVerificationService.openVerificationSession(
                caregiverMembership,
                visit.getId(),
                executionSession.getId(),
                OffsetDateTime.parse("2026-04-15T09:01:30-05:00"));
        evvVerificationService.recordClockEvent(caregiverMembership, verificationSession.getId(), new RecordClockEventCommand(
                EvvClockEventType.CLOCK_IN,
                OffsetDateTime.parse("2026-04-15T09:01:00-05:00"),
                BigDecimal.valueOf(41.881001),
                BigDecimal.valueOf(-87.623001),
                "America/Chicago",
                "mobile_app",
                null, null, null, null, null, null));

        assertThatThrownBy(() -> evvVerificationService.recordClockEvent(caregiverMembership, verificationSession.getId(), new RecordClockEventCommand(
                EvvClockEventType.CLOCK_IN,
                OffsetDateTime.parse("2026-04-15T09:02:00-05:00"),
                BigDecimal.valueOf(41.881010),
                BigDecimal.valueOf(-87.623010),
                "America/Chicago",
                "mobile_app",
                null, null, null, null, null, null)))
                .isInstanceOf(EvvConflictException.class);

        assertThatThrownBy(() -> evvVerificationService.openVerificationSession(
                otherCaregiverMembership,
                visit.getId(),
                executionSession.getId(),
                OffsetDateTime.parse("2026-04-15T09:01:30-05:00")))
                .isInstanceOf(UnauthorizedEvvActorException.class);

        assertThat(otherCaregiverProfile.getId()).isNotEqualTo(caregiverProfile.getId());
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(Agency.create("North Star Home Care", slug, "America/Chicago", "ops@northstar.example"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Test", role.name(), email, null));
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
