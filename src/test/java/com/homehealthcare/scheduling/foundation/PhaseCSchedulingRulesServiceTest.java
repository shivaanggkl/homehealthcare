package com.homehealthcare.scheduling.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityApprovalStatus;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityReasonType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredential;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialStatus;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialVerificationStatus;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreference;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceRepository;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceType;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfile;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfileRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfile;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfileRepository;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patient.domain.PatientRepository;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.scheduling.application.SchedulingConflictException;
import com.homehealthcare.scheduling.application.SchedulingRecordService;
import com.homehealthcare.scheduling.application.SchedulingRecordService.AssignCaregiverCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.ManageVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRecordService.RescheduleVisitCommand;
import com.homehealthcare.scheduling.application.SchedulingRulesService;
import com.homehealthcare.scheduling.application.SchedulingRulesService.CaregiverMatchResult;
import com.homehealthcare.scheduling.application.SchedulingRulesService.ConflictCheckCommand;
import com.homehealthcare.scheduling.application.SchedulingRulesService.MatchCaregiversCommand;
import com.homehealthcare.scheduling.application.SchedulingRulesService.OvertimeEvaluation;
import com.homehealthcare.scheduling.application.SchedulingRulesService.SchedulingConflictEvaluation;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.serviceline.domain.ServiceLineRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.visittype.domain.VisitType;
import com.homehealthcare.visittype.domain.VisitTypeRepository;
import java.math.BigDecimal;
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
class PhaseCSchedulingRulesServiceTest {

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
    private PatientAddressRepository patientAddressRepository;

    @Autowired
    private ServiceLineRepository serviceLineRepository;

    @Autowired
    private VisitTypeRepository visitTypeRepository;

    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;

    @Autowired
    private CaregiverAvailabilityRepository caregiverAvailabilityRepository;

    @Autowired
    private CaregiverUnavailabilityRepository caregiverUnavailabilityRepository;

    @Autowired
    private CaregiverCertificationRepository caregiverCertificationRepository;

    @Autowired
    private CaregiverCredentialRepository caregiverCredentialRepository;

    @Autowired
    private CaregiverLanguageProfileRepository caregiverLanguageProfileRepository;

    @Autowired
    private CaregiverSkillRepository caregiverSkillRepository;

    @Autowired
    private CaregiverSkillProfileRepository caregiverSkillProfileRepository;

    @Autowired
    private CaregiverGeographyPreferenceRepository caregiverGeographyPreferenceRepository;

    @Autowired
    private SchedulingRecordService schedulingRecordService;

    @Autowired
    private SchedulingRulesService schedulingRulesService;

    @Autowired
    private CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void schedulingRulesProvideStructuredMatchConflictTravelAndOvertimeResults() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-301",
                "Ava",
                null,
                "Patient",
                null,
                LocalDate.of(1950, 5, 20),
                "F",
                null,
                null,
                null,
                "en-US",
                null));
        patientAddressRepository.saveAndFlush(PatientAddress.create(
                patient,
                "1 Main St",
                null,
                "Chicago",
                "IL",
                "60601",
                "USA",
                new BigDecimal("41.881832"),
                new BigDecimal("-87.623177"),
                "GEOCODED",
                "America/Chicago",
                null));

        ServiceLine serviceLine = serviceLineRepository.saveAndFlush(ServiceLine.create(agency, "Skilled Nursing", "SN", "Skilled", 1));
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine", 60, true, 1));
        CaregiverSkill woundCare = caregiverSkillRepository.saveAndFlush(CaregiverSkill.create(agency, "Wound Care", "WC", "Wound care"));
        CaregiverCertification certification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agency, "RN", "RN", "Registered nurse", true));

        AgencyMembership caregiverOneMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver1@northstar.example");
        AgencyMembership caregiverTwoMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver2@northstar.example");

        CaregiverProfile caregiverOne = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverOneMembership, branch, "CG-101", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        CaregiverProfile caregiverTwo = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverTwoMembership, branch, "CG-102", "Robin Relief", "Part Time", LocalDate.of(2026, 1, 1), null, null));

        caregiverCredentialRepository.saveAndFlush(CaregiverCredential.create(
                caregiverOne,
                certification,
                "RN",
                "LIC-001",
                "Illinois Board",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                CaregiverCredentialStatus.ACTIVE,
                CaregiverCredentialVerificationStatus.VERIFIED,
                null));
        caregiverLanguageProfileRepository.saveAndFlush(CaregiverLanguageProfile.create(caregiverOne, "en-US", "Fluent", true));
        caregiverSkillProfileRepository.saveAndFlush(CaregiverSkillProfile.create(caregiverOne, woundCare, "Expert", true, null));
        caregiverAvailabilityRepository.saveAndFlush(CaregiverAvailability.create(
                caregiverOne,
                branch,
                CaregiverAvailabilityType.RECURRING,
                null,
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null));
        caregiverGeographyPreferenceRepository.saveAndFlush(CaregiverGeographyPreference.create(
                caregiverOne,
                null,
                CaregiverGeographyPreferenceType.RADIUS,
                null,
                null,
                null,
                new BigDecimal("41.880000"),
                new BigDecimal("-87.630000"),
                new BigDecimal("10.0"),
                1,
                null));

        caregiverUnavailabilityRepository.saveAndFlush(CaregiverUnavailability.create(
                caregiverTwo,
                CaregiverUnavailabilityReasonType.PTO,
                OffsetDateTime.parse("2026-04-13T15:00:00-05:00"),
                OffsetDateTime.parse("2026-04-13T18:00:00-05:00"),
                false,
                CaregiverUnavailabilityApprovalStatus.APPROVED,
                null));

        VisitOccurrence existingAssignmentVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-13T08:00:00-05:00"),
                OffsetDateTime.parse("2026-04-13T15:00:00-05:00"),
                "America/Chicago",
                "routine",
                "manual",
                null));
        schedulingRecordService.assignCaregiver(owner, existingAssignmentVisit.getId(), new AssignCaregiverCommand(
                caregiverOne.getId(),
                branch.getId(),
                "board",
                null));

        VisitOccurrence candidateVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-13T15:30:00-05:00"),
                OffsetDateTime.parse("2026-04-13T16:30:00-05:00"),
                "America/Chicago",
                "urgent",
                "manual",
                null));

        List<CaregiverMatchResult> matches = schedulingRulesService.matchCaregivers(owner, candidateVisit.getId(), new MatchCaregiversCommand(
                Set.of(woundCare.getId()),
                Set.of("RN"),
                "en-US",
                false,
                true,
                null,
                null));

        SchedulingConflictEvaluation blockedPreview = schedulingRulesService.previewAssignment(owner, candidateVisit.getId(), caregiverTwo.getId(), new ConflictCheckCommand(
                Set.of(woundCare.getId()),
                Set.of("RN"),
                "en-US",
                false,
                true,
                null,
                null));

        SchedulingTravelAwareness travelAwareness = schedulingRulesService.evaluateTravelAwareness(owner, candidateVisit.getId(), caregiverOne.getId());
        OvertimeEvaluation overtimeEvaluation = schedulingRulesService.evaluateOvertime(
                owner,
                caregiverOne.getId(),
                OffsetDateTime.parse("2026-04-13T17:00:00-05:00"),
                OffsetDateTime.parse("2026-04-13T19:00:00-05:00"),
                null);

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());

        assertThat(matches).hasSize(2);
        assertThat(matches.get(0).caregiverProfileId()).isEqualTo(caregiverOne.getId());
        assertThat(matches.get(0).outcome()).isEqualTo(SchedulingConflictOutcome.CLEAR);
        assertThat(blockedPreview.outcome()).isEqualTo(SchedulingConflictOutcome.BLOCKING);
        assertThat(blockedPreview.items())
                .extracting(SchedulingRulesService.ConflictItem::code)
                .contains("CAREGIVER_UNAVAILABLE", "MISSING_REQUIRED_SKILL", "MISSING_REQUIRED_CREDENTIAL", "NO_AVAILABILITY_PROFILE", "LANGUAGE_MISMATCH");
        assertThat(travelAwareness.level()).isNotEqualTo(TravelAwarenessLevel.UNKNOWN);
        assertThat(overtimeEvaluation.outcome()).isEqualTo(SchedulingConflictOutcome.WARNING);
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic5SchedulingAuditAction.CONFLICT_FLAGGED.actionType(),
                        Epic5SchedulingAuditAction.TRAVEL_EVALUATED.actionType());
    }

    @Test
    void schedulingRecordServiceUsesPhaseCRulesToBlockUnavailableAndOverlappingAssignments() {
        Agency agency = persistAgency("north-star-home-care");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverOneMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver1@northstar.example");
        AgencyMembership caregiverTwoMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver2@northstar.example");

        CaregiverProfile caregiverOne = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverOneMembership, branch, "CG-201", "Casey Care", "Full Time", LocalDate.of(2026, 1, 1), null, null));
        CaregiverProfile caregiverTwo = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                caregiverTwoMembership, branch, "CG-202", "Robin Relief", "Part Time", LocalDate.of(2026, 1, 1), null, null));

        caregiverUnavailabilityRepository.saveAndFlush(CaregiverUnavailability.create(
                caregiverOne,
                CaregiverUnavailabilityReasonType.PTO,
                OffsetDateTime.parse("2026-04-14T09:00:00-05:00"),
                OffsetDateTime.parse("2026-04-14T12:00:00-05:00"),
                false,
                CaregiverUnavailabilityApprovalStatus.APPROVED,
                null));

        Patient patient = patientRepository.saveAndFlush(Patient.create(
                agency,
                "PAT-302",
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
        VisitType visitType = visitTypeRepository.saveAndFlush(VisitType.create(agency, serviceLine, "Routine Visit", "RV", "Routine", 60, true, 1));

        VisitOccurrence firstVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-14T09:30:00-05:00"),
                OffsetDateTime.parse("2026-04-14T10:30:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));

        assertThatThrownBy(() -> schedulingRecordService.assignCaregiver(owner, firstVisit.getId(), new AssignCaregiverCommand(
                        caregiverOne.getId(),
                        branch.getId(),
                        "board",
                        null)))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessage("Caregiver has an overlapping unavailability window.");

        VisitOccurrence baseVisit = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-14T12:00:00-05:00"),
                OffsetDateTime.parse("2026-04-14T13:00:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, baseVisit.getId(), new AssignCaregiverCommand(
                caregiverTwo.getId(),
                branch.getId(),
                "board",
                null));

        VisitOccurrence rescheduleTarget = schedulingRecordService.createVisit(owner, new ManageVisitCommand(
                patient.getId(),
                branch.getId(),
                serviceLine.getId(),
                visitType.getId(),
                OffsetDateTime.parse("2026-04-14T14:30:00-05:00"),
                OffsetDateTime.parse("2026-04-14T15:30:00-05:00"),
                "America/Chicago",
                null,
                null,
                null));
        schedulingRecordService.assignCaregiver(owner, rescheduleTarget.getId(), new AssignCaregiverCommand(
                caregiverOne.getId(),
                branch.getId(),
                "board",
                null));

        assertThatThrownBy(() -> schedulingRecordService.rescheduleVisit(owner, rescheduleTarget.getId(), new RescheduleVisitCommand(
                        OffsetDateTime.parse("2026-04-14T12:30:00-05:00"),
                        OffsetDateTime.parse("2026-04-14T13:30:00-05:00"),
                        "America/Chicago",
                        branch.getId(),
                        caregiverTwo.getId(),
                        "move into occupied slot")))
                .isInstanceOf(SchedulingConflictException.class)
                .hasMessage("Caregiver already has an overlapping active visit assignment.");
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
