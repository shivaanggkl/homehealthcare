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
import com.homehealthcare.scheduling.foundation.SchedulingVisitStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingopenshift.domain.OpenShift;
import com.homehealthcare.schedulingopenshift.domain.OpenShiftRepository;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitCadence;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRule;
import com.homehealthcare.schedulingrecurrence.domain.RecurringVisitRuleRepository;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationEvent;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationEventRepository;
import com.homehealthcare.schedulingworkflow.domain.VisitCancellationParty;
import com.homehealthcare.schedulingworkflow.domain.VisitRescheduleEvent;
import com.homehealthcare.schedulingworkflow.domain.VisitRescheduleEventRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
class PhaseBSchedulingRepositoryTest {

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
    private RecurringVisitRuleRepository recurringVisitRuleRepository;

    @Autowired
    private VisitOccurrenceRepository visitOccurrenceRepository;

    @Autowired
    private CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;

    @Autowired
    private OpenShiftRepository openShiftRepository;

    @Autowired
    private VisitRescheduleEventRepository visitRescheduleEventRepository;

    @Autowired
    private VisitCancellationEventRepository visitCancellationEventRepository;

    @Test
    void recurringRuleVisitOccurrenceAndWorkflowEntitiesNormalizeAndPersistPhaseBModel() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                " PAT-001 ",
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
                "Test patient"));
        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Personal Care", "PC", "Personal care", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "rv", "Routine", 60, true, 1));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        CaregiverProfile caregiverProfile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverMembership,
                branch,
                " cg-001 ",
                " Casey Care ",
                " part time ",
                LocalDate.of(2026, 1, 1),
                null,
                null));

        RecurringVisitRule rule = recurringVisitRuleRepository.saveAndFlush(RecurringVisitRule.create(
                patient,
                branch,
                serviceLine,
                visitType,
                RecurringVisitCadence.SELECTED_WEEKDAYS,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "America/Chicago",
                " high ",
                " recurring ",
                " weekday rule "));

        VisitOccurrence occurrence = visitOccurrenceRepository.saveAndFlush(VisitOccurrence.create(
                patient,
                branch,
                serviceLine,
                visitType,
                rule,
                OffsetDateTime.parse("2026-04-06T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00-05:00"),
                "America/Chicago",
                " routine ",
                " manual ",
                " planned "));

        CaregiverVisitAssignment assignment = caregiverVisitAssignmentRepository.saveAndFlush(CaregiverVisitAssignment.create(
                occurrence,
                caregiverProfile,
                branch,
                owner,
                " board ",
                " assigned "));

        VisitOccurrence openVisit = visitOccurrenceRepository.saveAndFlush(VisitOccurrence.create(
                patient,
                branch,
                serviceLine,
                visitType,
                null,
                OffsetDateTime.parse("2026-04-07T11:00:00-05:00"),
                OffsetDateTime.parse("2026-04-07T12:00:00-05:00"),
                "America/Chicago",
                " urgent ",
                " manual ",
                null));
        OpenShift openShift = openShiftRepository.saveAndFlush(OpenShift.create(openVisit, branch, owner, " urgent ", " needs coverage "));

        VisitRescheduleEvent rescheduleEvent = visitRescheduleEventRepository.saveAndFlush(VisitRescheduleEvent.create(
                occurrence,
                caregiverProfile,
                caregiverProfile,
                owner,
                OffsetDateTime.parse("2026-04-06T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-06T10:00:00-05:00"),
                OffsetDateTime.parse("2026-04-06T13:00:00-05:00"),
                OffsetDateTime.parse("2026-04-06T14:00:00-05:00"),
                " caregiver requested shift "));

        VisitCancellationEvent cancellationEvent = visitCancellationEventRepository.saveAndFlush(VisitCancellationEvent.create(
                occurrence,
                owner,
                VisitCancellationParty.ADMIN_SIDE,
                " admin closed visit "));

        assertThat(rule.weekdays()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        assertThat(rule.getPriority()).isEqualTo("HIGH");
        assertThat(occurrence.getPriority()).isEqualTo("ROUTINE");
        assertThat(occurrence.getCreationMode()).isEqualTo("MANUAL");
        assertThat(assignment.getAssignmentStatus()).isEqualTo(CaregiverAssignmentStatus.ACTIVE);
        assertThat(openShift.getPriority()).isEqualTo("URGENT");
        assertThat(rescheduleEvent.getReason()).isEqualTo("caregiver requested shift");
        assertThat(cancellationEvent.getCancellationParty()).isEqualTo(VisitCancellationParty.ADMIN_SIDE);
        assertThat(occurrence.getStatus()).isEqualTo(SchedulingVisitStatus.PLANNED);
        assertThat(caregiverProfile.getStatus()).isEqualTo(WorkforceLifecycleStatus.ACTIVE);
    }

    @Test
    void recurringRuleAndVisitOccurrenceValidateStateCleanly() {
        Agency agency = persistAgency("north-star-home-care");
        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-002",
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

        assertThatThrownBy(() -> recurringVisitRuleRepository.saveAndFlush(RecurringVisitRule.create(
                        patient,
                        null,
                        null,
                        null,
                        RecurringVisitCadence.SELECTED_WEEKDAYS,
                        Set.of(),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 30),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        "America/Chicago",
                        null,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("weekdayPattern must be provided for SELECTED_WEEKDAYS cadence");

        assertThatThrownBy(() -> visitOccurrenceRepository.saveAndFlush(VisitOccurrence.create(
                        patient,
                        null,
                        null,
                        null,
                        null,
                        OffsetDateTime.parse("2026-04-06T10:00:00-05:00"),
                        OffsetDateTime.parse("2026-04-06T09:00:00-05:00"),
                        "America/Chicago",
                        null,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plannedEndAt must be after plannedStartAt");
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
