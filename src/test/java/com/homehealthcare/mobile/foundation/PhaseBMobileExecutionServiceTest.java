package com.homehealthcare.mobile.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.mobile.application.MobileConflictException;
import com.homehealthcare.mobile.application.MobileExecutionService;
import com.homehealthcare.mobile.application.MobileExecutionService.CaregiverRouteProjection;
import com.homehealthcare.mobile.application.MobileExecutionService.CaregiverTodayWorkItem;
import com.homehealthcare.mobile.application.MobileExecutionService.EndVisitExecutionCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.MobileCareInstructionSummary;
import com.homehealthcare.mobile.application.MobileExecutionService.MobilePatientSummary;
import com.homehealthcare.mobile.application.MobileExecutionService.SaveQuickNoteCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.SaveTaskChecklistItemCommand;
import com.homehealthcare.mobile.application.MobileExecutionService.StartVisitExecutionCommand;
import com.homehealthcare.mobile.application.UnauthorizedMobileActorException;
import com.homehealthcare.mobile.domain.MobileQuickNoteEntry;
import com.homehealthcare.mobile.domain.MobileVisitExecutionSession;
import com.homehealthcare.mobile.domain.MobileVisitTaskChecklistEntry;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.patientcontact.domain.PatientContact;
import com.homehealthcare.patientcontact.domain.PatientContactRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisCondition;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisConditionRepository;
import com.homehealthcare.patientdiagnosis.domain.PatientDiagnosisStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.tasktemplate.domain.TaskTemplate;
import com.homehealthcare.tasktemplate.domain.TaskTemplateCategory;
import com.homehealthcare.tasktemplate.domain.TaskTemplateRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
class PhaseBMobileExecutionServiceTest {

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
    private PatientContactRepository patientContactRepository;

    @Autowired
    private PatientDiagnosisConditionRepository patientDiagnosisConditionRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private TaskTemplateRepository taskTemplateRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private MobileExecutionService mobileExecutionService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void mobileExecutionServiceSupportsTodayWorkExecutionRouteSummaryTasksAndQuickNotesWithAudit() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-300",
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
        patientAddressRepository.saveAndFlush(PatientAddress.create(
                patient,
                "123 Main St",
                null,
                "Chicago",
                "IL",
                "60601",
                "USA",
                null,
                null,
                null,
                "America/Chicago",
                "Use side entrance"));
        patientContactRepository.saveAndFlush(PatientContact.create(
                patient,
                "Daughter",
                "Maya Patient",
                "312-555-0199",
                "maya@example.com",
                null,
                true,
                true,
                false,
                null));
        patientDiagnosisConditionRepository.saveAndFlush(PatientDiagnosisCondition.create(
                patient,
                "I10",
                "Hypertension",
                "Primary",
                true,
                LocalDate.of(2024, 1, 1),
                null,
                PatientDiagnosisStatus.ACTIVE,
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine visit instructions", 60, true, 1));
        TaskTemplate taskTemplate = taskTemplateRepository.saveAndFlush(TaskTemplate.create(
                agency,
                serviceLine,
                visitType,
                "Vitals",
                "VITALS",
                "Collect vitals",
                TaskTemplateCategory.CLINICAL,
                1));

        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-10T10:00:00-05:00"),
                "America/Chicago",
                "urgent",
                "manual",
                "Knock before entering"));
        CaregiverVisitAssignment assignment = schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                branch.getId(),
                "board",
                "assigned to caregiver"));

        List<CaregiverTodayWorkItem> todayWork = mobileExecutionService.getTodayWork(
                caregiverMembership,
                LocalDate.of(2026, 4, 10),
                "America/Chicago");
        CaregiverRouteProjection routeProjection = mobileExecutionService.getRouteProjection(
                caregiverMembership,
                LocalDate.of(2026, 4, 10),
                "America/Chicago");
        MobilePatientSummary patientSummary = mobileExecutionService.getPatientSummary(caregiverMembership, visit.getId());
        MobileCareInstructionSummary careInstructionSummary = mobileExecutionService.getCareInstructionSummary(caregiverMembership, visit.getId());

        MobileVisitExecutionSession session = mobileExecutionService.startVisitExecution(
                caregiverMembership,
                visit.getId(),
                new StartVisitExecutionCommand(
                        OffsetDateTime.parse("2026-04-10T09:02:00-05:00"),
                        BigDecimal.valueOf(41.881000),
                        BigDecimal.valueOf(-87.623000),
                        "mobile_app",
                        null));

        List<MobileVisitTaskChecklistEntry> checklistEntries = mobileExecutionService.saveTaskChecklist(
                caregiverMembership,
                session.getId(),
                List.of(new SaveTaskChecklistItemCommand(
                        taskTemplate.getId(),
                        "Vitals",
                        "Collect vitals",
                        TaskTemplateCategory.CLINICAL,
                        1,
                        true,
                        OffsetDateTime.parse("2026-04-10T09:15:00-05:00"),
                        "Completed successfully")));

        MobileQuickNoteEntry quickNote = mobileExecutionService.saveQuickNote(
                caregiverMembership,
                session.getId(),
                new SaveQuickNoteCommand(
                        com.homehealthcare.mobile.domain.MobileQuickNoteStatus.SUBMITTED,
                        "Patient resting comfortably.",
                        OffsetDateTime.parse("2026-04-10T09:20:00-05:00")));

        MobileVisitExecutionSession completedSession = mobileExecutionService.endVisitExecution(
                caregiverMembership,
                session.getId(),
                new EndVisitExecutionCommand(
                        OffsetDateTime.parse("2026-04-10T09:58:00-05:00"),
                        BigDecimal.valueOf(41.881100),
                        BigDecimal.valueOf(-87.623100),
                        "mobile_app",
                        MobileSyncDisposition.ACCEPTED));

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());

        assertThat(assignment.getId()).isNotNull();
        assertThat(todayWork).hasSize(1);
        assertThat(todayWork.get(0).visitId()).isEqualTo(visit.getId());
        assertThat(todayWork.get(0).routeOrder()).isEqualTo(1);
        assertThat(routeProjection.stops()).hasSize(1);
        assertThat(routeProjection.stops().get(0).addressSummary()).contains("123 Main St");
        assertThat(patientSummary.patientDisplaySummary()).isEqualTo("Ava Patient");
        assertThat(patientSummary.contactSummary().fullName()).isEqualTo("Maya Patient");
        assertThat(patientSummary.diagnosisSummaries()).contains("Hypertension");
        assertThat(careInstructionSummary.visitTypeInstructions()).isEqualTo("Routine visit instructions");
        assertThat(careInstructionSummary.patientSpecificCareNotes()).isEqualTo("Knock before entering");
        assertThat(checklistEntries).hasSize(1);
        assertThat(checklistEntries.get(0).isCompleted()).isTrue();
        assertThat(quickNote.getStatus()).isEqualTo(com.homehealthcare.mobile.domain.MobileQuickNoteStatus.SUBMITTED);
        assertThat(completedSession.getExecutionStatus()).isEqualTo(MobileExecutionSessionStatus.COMPLETED);
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic6MobileAuditAction.VISIT_EXECUTION_STARTED.actionType(),
                        Epic6MobileAuditAction.TASK_CHECKLIST_SAVED.actionType(),
                        Epic6MobileAuditAction.QUICK_NOTE_SAVED.actionType(),
                        Epic6MobileAuditAction.VISIT_EXECUTION_ENDED.actionType());
    }

    @Test
    void mobileExecutionServiceRejectsDuplicateActiveSessionsAndUnauthorizedAccess() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        AgencyMembership otherCaregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "other@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                otherCaregiverMembership, branch, "CG-002", "Robin Relief", "Part Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-301",
                "Mila",
                null,
                "Patient",
                null,
                LocalDate.of(1952, 1, 10),
                "F",
                null,
                null,
                null,
                "en-US",
                null));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Care Visit", "CV", "Care visit", 60, true, 1));
        var visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-12T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-12T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverProfile.getId(),
                branch.getId(),
                "board",
                null));

        MobileVisitExecutionSession session = mobileExecutionService.startVisitExecution(
                caregiverMembership,
                visit.getId(),
                new StartVisitExecutionCommand(
                        OffsetDateTime.parse("2026-04-12T09:01:00-05:00"),
                        null,
                        null,
                        "mobile_app",
                        null));

        assertThatThrownBy(() -> mobileExecutionService.startVisitExecution(
                        caregiverMembership,
                        visit.getId(),
                        new StartVisitExecutionCommand(
                                OffsetDateTime.parse("2026-04-12T09:05:00-05:00"),
                                null,
                                null,
                                "mobile_app",
                                null)))
                .isInstanceOf(MobileConflictException.class)
                .hasMessage("An in-progress mobile execution session already exists for this visit.");

        assertThatThrownBy(() -> mobileExecutionService.getPatientSummary(otherCaregiverMembership, visit.getId()))
                .isInstanceOf(UnauthorizedMobileActorException.class);

        assertThat(session.getId()).isNotNull();
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(
                Agency.create("Agency " + slug, slug, "America/Chicago", slug + "@example.com"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Mobile", "Actor", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
