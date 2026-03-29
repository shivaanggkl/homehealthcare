package com.homehealthcare.workforce.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityApprovalStatus;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityReasonType;
import com.homehealthcare.caregivercredential.domain.CaregiverCredential;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialStatus;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialVerificationStatus;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.workforce.application.CaregiverWorkforceService;
import com.homehealthcare.workforce.application.CaregiverWorkforceService.ManageCaregiverAvailabilityCommand;
import com.homehealthcare.workforce.application.CaregiverWorkforceService.ManageCaregiverCredentialCommand;
import com.homehealthcare.workforce.application.CaregiverWorkforceService.ManageCaregiverLanguageCommand;
import com.homehealthcare.workforce.application.CaregiverWorkforceService.ManageCaregiverProfileCommand;
import com.homehealthcare.workforce.application.CaregiverWorkforceService.ManageCaregiverSkillCommand;
import com.homehealthcare.workforce.application.CaregiverWorkforceService.ManageCaregiverUnavailabilityCommand;
import com.homehealthcare.workforce.application.UnauthorizedWorkforceActorException;
import com.homehealthcare.workforce.application.WorkforceConflictException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
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
class PhaseBWorkforceServiceTest {

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private com.homehealthcare.certification.domain.CaregiverCertificationRepository caregiverCertificationRepository;

    @Autowired
    private com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository caregiverSkillRepository;

    @Autowired
    private CaregiverWorkforceService caregiverWorkforceService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void workforceServicesPersistPhaseBRecordsAndAuditThem() {
        Agency agency = persistAgency("north-star-home-care");
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        CaregiverCertification certification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agency, "CPR", "CPR", "CPR certification", true));
        CaregiverSkill skill = caregiverSkillRepository.saveAndFlush(CaregiverSkill.create(agency, "Wound Care", "WC", "Wound care"));

        CaregiverProfile profile = caregiverWorkforceService.createProfile(owner, new ManageCaregiverProfileCommand(
                caregiverMembership.getId(),
                branch.getId(),
                "CG-001",
                "Casey Care",
                "Part Time",
                LocalDate.of(2026, 1, 1),
                null,
                "Reliable weekend caregiver"));

        CaregiverCredential credential = caregiverWorkforceService.createCredential(owner, profile.getId(), new ManageCaregiverCredentialCommand(
                certification.getId(),
                "RN",
                "LIC-123",
                "Illinois Board",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2027, 1, 1),
                CaregiverCredentialStatus.ACTIVE,
                CaregiverCredentialVerificationStatus.VERIFIED,
                "Verified on file"));

        caregiverWorkforceService.createLanguage(owner, profile.getId(), new ManageCaregiverLanguageCommand(
                "en-US",
                "Fluent",
                true));

        caregiverWorkforceService.createSkill(owner, profile.getId(), new ManageCaregiverSkillCommand(
                skill.getId(),
                "Expert",
                true,
                "Validated"));

        caregiverWorkforceService.createAvailability(owner, profile.getId(), new ManageCaregiverAvailabilityCommand(
                branch.getId(),
                CaregiverAvailabilityType.RECURRING,
                null,
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "Morning visits"));

        caregiverWorkforceService.createUnavailability(owner, profile.getId(), new ManageCaregiverUnavailabilityCommand(
                CaregiverUnavailabilityReasonType.PTO,
                OffsetDateTime.parse("2026-04-10T08:00:00-05:00"),
                OffsetDateTime.parse("2026-04-12T17:00:00-05:00"),
                true,
                CaregiverUnavailabilityApprovalStatus.APPROVED,
                "Vacation"));

        caregiverWorkforceService.deactivateProfile(owner, profile.getId());

        List<AuditEvent> events = auditEventRepository.findAllByAgencyIdOrderByOccurredAtAsc(agency.getId());
        assertThat(events)
                .extracting(AuditEvent::getTargetType)
                .contains(
                        Epic4WorkforceTargetType.CAREGIVER_PROFILE.name(),
                        Epic4WorkforceTargetType.CAREGIVER_CREDENTIAL.name(),
                        Epic4WorkforceTargetType.CAREGIVER_LANGUAGE.name(),
                        Epic4WorkforceTargetType.CAREGIVER_SKILL.name(),
                        Epic4WorkforceTargetType.CAREGIVER_AVAILABILITY.name(),
                        Epic4WorkforceTargetType.CAREGIVER_UNAVAILABILITY.name());
        assertThat(events)
                .extracting(AuditEvent::getActionType)
                .contains(
                        Epic4WorkforceAuditAction.CREATED.actionType(),
                        Epic4WorkforceAuditAction.DEACTIVATED.actionType());

        assertThat(credential.getStatus()).isEqualTo(CaregiverCredentialStatus.ACTIVE);
        assertThat(profile.getStatus()).isEqualTo(WorkforceLifecycleStatus.INACTIVE);
    }

    @Test
    void workforceServicesRejectDuplicateProfilesAndOverlappingWindowsAndUnauthorizedActors() {
        Agency agency = persistAgency("north-star-home-care");
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverActor = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver-actor@northstar.example");
        AgencyMembership subjectMembership = persistMembership(agency, AgencyRole.CAREGIVER, "subject@northstar.example");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));

        CaregiverProfile profile = caregiverWorkforceService.createProfile(owner, new ManageCaregiverProfileCommand(
                subjectMembership.getId(),
                branch.getId(),
                "CG-001",
                "Casey Care",
                null,
                LocalDate.of(2026, 1, 1),
                null,
                null));

        assertThatThrownBy(() -> caregiverWorkforceService.createProfile(owner, new ManageCaregiverProfileCommand(
                        subjectMembership.getId(),
                        null,
                        "CG-002",
                        "Duplicate",
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(WorkforceConflictException.class)
                .hasMessage("An active caregiver profile already exists for this membership.");

        caregiverWorkforceService.createAvailability(owner, profile.getId(), new ManageCaregiverAvailabilityCommand(
                branch.getId(),
                CaregiverAvailabilityType.DATE_SPECIFIC,
                OffsetDateTime.parse("2026-04-10T08:00:00-05:00"),
                OffsetDateTime.parse("2026-04-10T12:00:00-05:00"),
                null,
                null,
                null,
                null,
                null,
                null));

        assertThatThrownBy(() -> caregiverWorkforceService.createAvailability(owner, profile.getId(), new ManageCaregiverAvailabilityCommand(
                        branch.getId(),
                        CaregiverAvailabilityType.DATE_SPECIFIC,
                        OffsetDateTime.parse("2026-04-10T11:00:00-05:00"),
                        OffsetDateTime.parse("2026-04-10T13:00:00-05:00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(WorkforceConflictException.class)
                .hasMessage("Availability overlaps an existing active availability window.");

        caregiverWorkforceService.createUnavailability(owner, profile.getId(), new ManageCaregiverUnavailabilityCommand(
                CaregiverUnavailabilityReasonType.SICK,
                OffsetDateTime.parse("2026-05-01T08:00:00-05:00"),
                OffsetDateTime.parse("2026-05-01T17:00:00-05:00"),
                false,
                CaregiverUnavailabilityApprovalStatus.PENDING,
                null));

        assertThatThrownBy(() -> caregiverWorkforceService.createUnavailability(owner, profile.getId(), new ManageCaregiverUnavailabilityCommand(
                        CaregiverUnavailabilityReasonType.PTO,
                        OffsetDateTime.parse("2026-05-01T12:00:00-05:00"),
                        OffsetDateTime.parse("2026-05-01T18:00:00-05:00"),
                        false,
                        CaregiverUnavailabilityApprovalStatus.PENDING,
                        null)))
                .isInstanceOf(WorkforceConflictException.class)
                .hasMessage("Unavailability overlaps an existing active unavailability window.");

        assertThatThrownBy(() -> caregiverWorkforceService.createProfile(caregiverActor, new ManageCaregiverProfileCommand(
                        UUID.randomUUID(),
                        null,
                        "CG-003",
                        "Forbidden",
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(UnauthorizedWorkforceActorException.class);
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(
                Agency.create("Agency " + slug, slug, "America/Chicago", slug + "@example.com"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Care", "Giver", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
