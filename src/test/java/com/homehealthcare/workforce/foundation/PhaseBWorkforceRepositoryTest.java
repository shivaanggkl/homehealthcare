package com.homehealthcare.workforce.foundation;

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
import com.homehealthcare.caregivercredential.domain.CaregiverCredential;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialStatus;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialVerificationStatus;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreference;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceType;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfile;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfileRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.caregivershift.domain.CaregiverShiftPreference;
import com.homehealthcare.caregivershift.domain.ShiftPreferenceStrength;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfile;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class PhaseBWorkforceRepositoryTest {

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
    private CaregiverCredentialRepository caregiverCredentialRepository;

    @Autowired
    private CaregiverLanguageProfileRepository caregiverLanguageProfileRepository;

    @Autowired
    private CaregiverSkillRepository caregiverSkillRepository;

    @Autowired
    private CaregiverCertificationRepository caregiverCertificationRepository;

    @Autowired
    private CaregiverAvailabilityRepository caregiverAvailabilityRepository;

    @Test
    void caregiverProfileNormalizesFieldsSupportsLifecycleAndIsUniquePerMembership() {
        Agency agency = persistAgency("north-star-home-care");
        AgencyMembership membership = persistMembership(agency, "owner@northstar.example");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));

        CaregiverProfile profile = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                membership,
                branch,
                " cg-001 ",
                " Casey Care ",
                " full time ",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                " dependable "));

        assertThat(profile.getCaregiverCode()).isEqualTo("CG-001");
        assertThat(profile.getDisplayName()).isEqualTo("Casey Care");
        assertThat(profile.getEmploymentType()).isEqualTo("full time");
        assertThat(profile.getStatus()).isEqualTo(WorkforceLifecycleStatus.ACTIVE);
        assertThat(profile.getUserId()).isEqualTo(membership.getUserId());

        profile.deactivate();
        caregiverProfileRepository.saveAndFlush(profile);
        assertThat(profile.getStatus()).isEqualTo(WorkforceLifecycleStatus.INACTIVE);

        assertThatThrownBy(() -> caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                        membership,
                        null,
                        "cg-002",
                        "Duplicate",
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void caregiverCredentialRequiresSameAgencyCertificationAndValidIssueWindow() {
        Agency agencyOne = persistAgency("north-star-home-care");
        Agency agencyTwo = persistAgency("sunrise-home-care");
        CaregiverProfile profile = persistProfile(agencyOne, "caregiver1@northstar.example");
        CaregiverCertification certification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agencyOne, "CPR", "CPR", "CPR certification", true));
        CaregiverCertification foreignCertification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agencyTwo, "HHA", "HHA", "Foreign", false));

        CaregiverCredential credential = caregiverCredentialRepository.saveAndFlush(CaregiverCredential.create(
                profile,
                certification,
                " RN ",
                " LIC-123 ",
                " State Board ",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                CaregiverCredentialStatus.ACTIVE,
                CaregiverCredentialVerificationStatus.VERIFIED,
                " cleared "));

        assertThat(credential.getCredentialType()).isEqualTo("RN");
        assertThat(credential.getLicenseNumber()).isEqualTo("LIC-123");
        assertThat(credential.getVerificationStatus()).isEqualTo(CaregiverCredentialVerificationStatus.VERIFIED);

        assertThatThrownBy(() -> CaregiverCredential.create(
                        profile,
                        foreignCertification,
                        "RN",
                        "LIC-456",
                        null,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 1, 1),
                        CaregiverCredentialStatus.ACTIVE,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("certification must belong to the same agency as the caregiver profile");

        assertThatThrownBy(() -> caregiverCredentialRepository.saveAndFlush(CaregiverCredential.create(
                        profile,
                        certification,
                        "RN",
                        "LIC-789",
                        null,
                        LocalDate.of(2027, 1, 2),
                        LocalDate.of(2027, 1, 1),
                        CaregiverCredentialStatus.ACTIVE,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("expiresOn must be on or after issuedOn");
    }

    @Test
    void caregiverLanguageSkillGeographyShiftAvailabilityAndUnavailabilityValidateCoreShape() {
        Agency agency = persistAgency("north-star-home-care");
        Agency foreignAgency = persistAgency("sunrise-home-care");
        CaregiverProfile profile = persistProfile(agency, "caregiver2@northstar.example");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "West Branch", "WB", "Naperville", "America/Chicago"));
        Branch foreignBranch = branchRepository.saveAndFlush(Branch.create(foreignAgency, "Foreign Branch", "FB", "Albany", "America/New_York"));
        CaregiverSkill skill = caregiverSkillRepository.saveAndFlush(CaregiverSkill.create(agency, "Wound Care", "WC", "Wound care"));
        CaregiverSkill foreignSkill = caregiverSkillRepository.saveAndFlush(CaregiverSkill.create(foreignAgency, "Skilled Nursing", "SN", "Foreign"));

        CaregiverLanguageProfile language = caregiverLanguageProfileRepository.saveAndFlush(
                CaregiverLanguageProfile.create(profile, " en-us ", " fluent ", true));
        assertThat(language.getLanguageCode()).isEqualTo("en-US");
        assertThat(language.getAgencyId()).isEqualTo(agency.getId());
        assertThat(language.isPrimaryLanguage()).isTrue();

        CaregiverSkillProfile skillProfile = CaregiverSkillProfile.create(profile, skill, " expert ", true, " verified ");
        assertThat(skillProfile.getSkill()).isEqualTo(skill);

        CaregiverGeographyPreference geography = CaregiverGeographyPreference.create(
                profile,
                branch,
                CaregiverGeographyPreferenceType.BRANCH,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                " prefers west");
        assertThat(geography.getPreferenceType()).isEqualTo(CaregiverGeographyPreferenceType.BRANCH);

        CaregiverShiftPreference shiftPreference = CaregiverShiftPreference.create(
                profile,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(16, 0),
                480,
                "SOC,ROC",
                ShiftPreferenceStrength.PREFERRED,
                " mornings ");
        assertThat(shiftPreference.getPreferenceStrength()).isEqualTo(ShiftPreferenceStrength.PREFERRED);

        CaregiverAvailability recurringAvailability = caregiverAvailabilityRepository.saveAndFlush(CaregiverAvailability.create(
                profile,
                branch,
                CaregiverAvailabilityType.RECURRING,
                null,
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                " recurring "));
        assertThat(recurringAvailability.getStatus()).isEqualTo(WorkforceLifecycleStatus.ACTIVE);

        CaregiverUnavailability unavailability = CaregiverUnavailability.create(
                profile,
                CaregiverUnavailabilityReasonType.PTO,
                OffsetDateTime.parse("2026-04-10T08:00:00-05:00"),
                OffsetDateTime.parse("2026-04-12T17:00:00-05:00"),
                true,
                CaregiverUnavailabilityApprovalStatus.APPROVED,
                " vacation ");
        assertThat(unavailability.getApprovalStatus()).isEqualTo(CaregiverUnavailabilityApprovalStatus.APPROVED);

        assertThatThrownBy(() -> caregiverLanguageProfileRepository.saveAndFlush(
                        CaregiverLanguageProfile.create(profile, "english", null, false)))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("Invalid caregiver language tag");

        assertThatThrownBy(() -> CaregiverSkillProfile.create(profile, foreignSkill, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("skill must belong to the same agency as the caregiver profile");

        assertThatThrownBy(() -> CaregiverGeographyPreference.create(
                        profile,
                        foreignBranch,
                        CaregiverGeographyPreferenceType.BRANCH,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("branch must belong to the same agency as the caregiver profile");

        assertThatThrownBy(() -> CaregiverGeographyPreference.create(
                        profile,
                        null,
                        CaregiverGeographyPreferenceType.RADIUS,
                        null,
                        null,
                        null,
                        new BigDecimal("41.0"),
                        null,
                        new BigDecimal("15.0"),
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("radius preference requires anchor coordinates and radius");

        assertThatThrownBy(() -> CaregiverShiftPreference.create(
                        profile,
                        DayOfWeek.MONDAY,
                        LocalTime.of(16, 0),
                        LocalTime.of(8, 0),
                        480,
                        null,
                        ShiftPreferenceStrength.AVOID,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("preferredEndTime must be after preferredStartTime");

        assertThatThrownBy(() -> caregiverAvailabilityRepository.saveAndFlush(CaregiverAvailability.create(
                        profile,
                        null,
                        CaregiverAvailabilityType.DATE_SPECIFIC,
                        OffsetDateTime.parse("2026-04-10T12:00:00-05:00"),
                        OffsetDateTime.parse("2026-04-10T08:00:00-05:00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endsAt must be after startsAt");

        assertThatThrownBy(() -> CaregiverUnavailability.create(
                        profile,
                        CaregiverUnavailabilityReasonType.SICK,
                        OffsetDateTime.parse("2026-04-12T17:00:00-05:00"),
                        OffsetDateTime.parse("2026-04-10T08:00:00-05:00"),
                        false,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endsAt must be after startsAt");
    }

    private Agency persistAgency(String slug) {
        return agencyRepository.saveAndFlush(
                Agency.create("Agency " + slug, slug, "America/Chicago", slug + "@example.com"));
    }

    private AgencyMembership persistMembership(Agency agency, String email) {
        User user = userRepository.saveAndFlush(User.invite("Care", "Giver", email, "312-555-0100"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.AGENCY_OWNER));
    }

    private CaregiverProfile persistProfile(Agency agency, String email) {
        AgencyMembership membership = persistMembership(agency, email);
        return caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                membership,
                null,
                "CG-" + membership.getId().toString().substring(0, 8),
                "Care Giver",
                "full time",
                LocalDate.of(2026, 1, 1),
                null,
                null));
    }
}
