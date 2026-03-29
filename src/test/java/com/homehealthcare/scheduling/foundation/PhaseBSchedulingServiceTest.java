package com.homehealthcare.scheduling.foundation;

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
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.application.SchedulingConflictException;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.CancelVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageOpenShiftCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageRecurringVisitRuleCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.RescheduleVisitCommand;
import com.homehealthcare.scheduling.application.UnauthorizedSchedulingActorException;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingopenshift.domain.OpenShift;
import com.homehealthcare.schedulingopenshift.domain.OpenShiftRepository;
import com.homehealthcare.schedulingopenshift.domain.OpenShiftStatus;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitCadence;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRule;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationEvent;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationParty;
import com.homehealthcare.schedulingworkflow.domain.VisitRescheduleEvent;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
class PhaseBSchedulingServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

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
    private CaregiverProfileRepository caregiverProfileRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private VisitOccurrenceRepository visitOccurrenceRepository;

    @Autowired
    private CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;

    @Autowired
    private OpenShiftRepository openShiftRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void schedulingServicePersistsRecurringVisitsAssignmentsOpenShiftsReschedulesAndCancellationsWithAudit() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverOneMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver1@northstar.example");
        AgencyMembership caregiverTwoMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver2@northstar.example");
        CaregiverProfile caregiverOne = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverOneMembership, branch, "CG-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        CaregiverProfile caregiverTwo = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverTwoMembership, branch, "CG-002", "Robin Relief", "Part Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-010",
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
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine", 60, true, 1));

        RecurringVisitRule recurringRule = schedulingRecordService.createRecurringRule(owner, new ManageRecurringVisitRuleCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                RecurringVisitCadence.SELECTED_WEEKDAYS,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 8),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "America/Chicago",
                "high",
                "recurring",
                "weekday rule"));

        List<VisitOccurrence> generated = schedulingRecordService.expandRecurringRule(
                owner,
                recurringRule.getId(),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 8));

        VisitOccurrence visit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-10T10:00:00-05:00"),
                "America/Chicago",
                "urgent",
                "manual",
                "created by coordinator"));

        OpenShift openShift = schedulingRecordService.openShift(owner, visit.getId(), new ManageOpenShiftCommand(
                branch.getId(),
                "urgent",
                "needs coverage"));

        CaregiverVisitAssignment assignment = schedulingRecordService.assignCaregiver(owner, visit.getId(), new AssignCaregiverCommand(
                caregiverOne.getId(),
                branch.getId(),
                "board",
                "assigned from board"));

        VisitRescheduleEvent rescheduleEvent = schedulingRecordService.rescheduleVisit(owner, visit.getId(), new RescheduleVisitCommand(
                OffsetDateTime.parse("2026-04-10T13:00:00-05:00"),
                OffsetDateTime.parse("2026-04-10T14:00:00-05:00"),
                "America/Chicago",
                branch.getId(),
                caregiverTwo.getId(),
                "patient requested afternoon"));

        VisitCancellationEvent cancellationEvent = schedulingRecordService.cancelVisit(owner, visit.getId(), new CancelVisitCommand(
                VisitCancellationParty.ADMIN_SIDE,
                "patient admitted to hospital"));

        VisitOccurrence savedVisit = visitOccurrenceRepository.findById(visit.getId()).orElseThrow();
        CaregiverVisitAssignment latestAssignment = caregiverVisitAssignmentRepository
                .findFirstByVisitOccurrence_IdAndAssignmentStatusOrderByAssignedAtDesc(visit.getId(), CaregiverAssignmentStatus.CANCELLED)
                .orElseThrow();
        OpenShift closedOpenShift = openShiftRepository.findById(openShift.getId()).orElseThrow();
        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());

        assertThat(recurringRule.getId()).isNotNull();
        assertThat(generated).hasSize(3);
        assertThat(assignment.getId()).isNotNull();
        assertThat(rescheduleEvent.getPreviousCaregiverProfile().getId()).isEqualTo(caregiverOne.getId());
        assertThat(rescheduleEvent.getNewCaregiverProfile().getId()).isEqualTo(caregiverTwo.getId());
        assertThat(cancellationEvent.getCancellationParty()).isEqualTo(VisitCancellationParty.ADMIN_SIDE);
        assertThat(savedVisit.getStatus()).isEqualTo(SchedulingVisitStatus.CANCELLED);
        assertThat(latestAssignment.getCaregiverProfileId()).isEqualTo(caregiverTwo.getId());
        assertThat(closedOpenShift.getStatus()).isEqualTo(OpenShiftStatus.CLAIMED_OR_ASSIGNED);
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic5SchedulingAuditAction.RECURRING_RULE_CREATED.actionType(),
                        Epic5SchedulingAuditAction.VISIT_CREATED.actionType(),
                        Epic5SchedulingAuditAction.OPEN_SHIFT_CREATED.actionType(),
                        Epic5SchedulingAuditAction.OPEN_SHIFT_CLOSED.actionType(),
                        Epic5SchedulingAuditAction.CAREGIVER_ASSIGNED.actionType(),
                        Epic5SchedulingAuditAction.VISIT_RESCHEDULED.actionType(),
                        Epic5SchedulingAuditAction.VISIT_CANCELLED.actionType());
    }

    @Test
    void schedulingServiceRejectsOverlappingAssignmentsAndUnauthorizedActors() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverActor = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver-actor@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver1@northstar.example");
        CaregiverProfile caregiver = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership, branch, "CG-001", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-020",
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
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled nursing", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Skilled Visit", "SV", "Skilled", 60, true, 1));

        VisitOccurrence firstVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-10T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-10T10:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        VisitOccurrence overlappingVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-10T09:30:00-05:00"),
                OffsetDateTime.parse("2026-04-10T10:30:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));

        schedulingRecordService.assignCaregiver(owner, firstVisit.getId(), new AssignCaregiverCommand(
                caregiver.getId(),
                branch.getId(),
                "board",
                null));

        assertThatThrownBy(() -> schedulingRecordService.assignCaregiver(owner, overlappingVisit.getId(), new AssignCaregiverCommand(
                        caregiver.getId(),
                        branch.getId(),
                        "board",
                        null)))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessage("Caregiver already has an overlapping active visit assignment.");

        assertThatThrownBy(() -> schedulingRecordService.createVisit(caregiverActor, new ManageVisitCommand(
                        patient.getId(),
                        branch.getId(),
                        serviceLine.getId(),
                        visitType.getId(),
                        OffsetDateTime.parse("2026-04-11T09:00:00-05:00"),
                        OffsetDateTime.parse("2026-04-11T10:00:00-05:00"),
                        "America/Chicago",
                        null,
                        null,
                        null)))
                .isInstanceOf(UnauthorizedSchedulingActorException.class);
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(
                Agency.create("Agency " + slug, slug, "America/Chicago", slug + "@example.com"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Schedule", "User", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
