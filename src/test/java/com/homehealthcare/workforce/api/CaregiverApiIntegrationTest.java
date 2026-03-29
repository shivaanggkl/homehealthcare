package com.homehealthcare.workforce.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.testsupport.TestTenantAuthentications;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CaregiverApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private CaregiverCertificationRepository caregiverCertificationRepository;

    @Autowired
    private CaregiverSkillRepository caregiverSkillRepository;

    @Autowired
    private CaregiverProfileRepository caregiverProfileRepository;

    @Autowired
    private CaregiverCredentialRepository caregiverCredentialRepository;

    @Test
    void ownerCanManageCaregiverDirectoryAndProfile() throws Exception {
        Agency agency = persistAgency();
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));

        mockMvc.perform(post("/api/caregivers")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "agencyMembershipId": "%s",
                                  "primaryBranchId": "%s",
                                  "caregiverCode": "CG-001",
                                  "displayName": "Casey Care",
                                  "employmentType": "Part Time",
                                  "startDate": "2026-01-01",
                                  "notes": "Weekend caregiver"
                                }
                                """.formatted(caregiverMembership.getId(), branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caregiverCode").value("CG-001"))
                .andExpect(jsonPath("$.userEmail").value("caregiver@northstar.example"))
                .andExpect(jsonPath("$.primaryBranchName").value("North Branch"));

        mockMvc.perform(get("/api/caregivers")
                        .param("search", "casey")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].displayName").value("Casey Care"))
                .andExpect(jsonPath("$.content[0].userEmail").value("caregiver@northstar.example"));

        UUID caregiverProfileId = caregiverProfileRepository
                .findAllByAgency_IdOrderByCreatedAtAsc(agency.getId())
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/caregivers/{caregiverId}", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipRole").value("CAREGIVER"));

        mockMvc.perform(put("/api/caregivers/{caregiverId}", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "agencyMembershipId": "%s",
                                  "primaryBranchId": "%s",
                                  "caregiverCode": "CG-001A",
                                  "displayName": "Casey Care Updated",
                                  "employmentType": "Full Time",
                                  "startDate": "2026-01-01",
                                  "endDate": "2026-12-31",
                                  "notes": "Updated"
                                }
                                """.formatted(caregiverMembership.getId(), branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Casey Care Updated"))
                .andExpect(jsonPath("$.employmentType").value("Full Time"));

        mockMvc.perform(delete("/api/caregivers/{caregiverId}", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void ownerCanManageCredentialsPreferencesAvailabilityAndPerformanceAndAuditIsQueryable() throws Exception {
        Agency agency = persistAgency();
        AgencyMembership owner = persistMembership(agency, AgencyRole.AGENCY_OWNER, "owner@northstar.example");
        AgencyMembership caregiverMembership = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "North Branch", "NB", "Chicago", "America/Chicago"));
        CaregiverCertification certification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agency, "CPR", "CPR", "CPR certification", true));
        CaregiverSkill skill = caregiverSkillRepository.saveAndFlush(
                CaregiverSkill.create(agency, "Wound Care", "WC", "Wound care"));

        mockMvc.perform(post("/api/caregivers")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "agencyMembershipId": "%s",
                                  "primaryBranchId": "%s",
                                  "caregiverCode": "CG-001",
                                  "displayName": "Casey Care",
                                  "employmentType": "Part Time",
                                  "startDate": "2026-01-01"
                                }
                                """.formatted(caregiverMembership.getId(), branch.getId())))
                .andExpect(status().isOk());

        UUID caregiverProfileId = caregiverProfileRepository
                .findAllByAgency_IdOrderByCreatedAtAsc(agency.getId())
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/caregivers/{caregiverId}/credentials", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "certificationId": "%s",
                                  "credentialType": "RN",
                                  "licenseNumber": "LIC-123",
                                  "issuingAuthority": "Illinois Board",
                                  "issuedOn": "2025-01-01",
                                  "expiresOn": "2027-01-01",
                                  "status": "ACTIVE",
                                  "verificationStatus": "VERIFIED",
                                  "notes": "Verified"
                                }
                                """.formatted(certification.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenseNumber").value("LIC-123"));

        UUID actualCredentialId = caregiverCredentialRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfileId)
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/caregivers/{caregiverId}/credentials/{credentialId}", caregiverProfileId, actualCredentialId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "certificationId": "%s",
                                  "credentialType": "RN",
                                  "licenseNumber": "LIC-456",
                                  "issuingAuthority": "Illinois Board",
                                  "issuedOn": "2025-01-01",
                                  "expiresOn": "2027-06-01",
                                  "status": "ACTIVE",
                                  "verificationStatus": "VERIFIED",
                                  "notes": "Updated"
                                }
                                """.formatted(certification.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenseNumber").value("LIC-456"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/languages", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "languageCode": "en-US",
                                  "proficiencyLevel": "Fluent",
                                  "primaryLanguage": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("en-US"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/skills", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "skillId": "%s",
                                  "proficiencyLevel": "Expert",
                                  "verified": true,
                                  "notes": "Validated"
                                }
                                """.formatted(skill.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillCode").value("WC"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/geography-preferences", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "preferenceType": "BRANCH",
                                  "priorityRank": 1,
                                  "notes": "Primary branch"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceType").value("BRANCH"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/shift-preferences", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dayOfWeek": "MONDAY",
                                  "preferredStartTime": "08:00:00",
                                  "preferredEndTime": "16:00:00",
                                  "preferredShiftLengthMinutes": 480,
                                  "preferredVisitTypes": "SOC,ROC",
                                  "preferenceStrength": "PREFERRED",
                                  "notes": "Day shift"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferenceStrength").value("PREFERRED"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/availabilities", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "availabilityType": "DATE_SPECIFIC",
                                  "startsAt": "2026-04-10T08:00:00-05:00",
                                  "endsAt": "2026-04-10T12:00:00-05:00",
                                  "notes": "Morning block"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityType").value("DATE_SPECIFIC"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/availabilities", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "availabilityType": "DATE_SPECIFIC",
                                  "startsAt": "2026-04-10T11:00:00-05:00",
                                  "endsAt": "2026-04-10T13:00:00-05:00",
                                  "notes": "Overlap"
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/caregivers/{caregiverId}/unavailabilities", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonType": "PTO",
                                  "startsAt": "2026-05-01T08:00:00-05:00",
                                  "endsAt": "2026-05-01T17:00:00-05:00",
                                  "allDay": false,
                                  "approvalStatus": "APPROVED",
                                  "notes": "Vacation"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasonType").value("PTO"));

        mockMvc.perform(post("/api/caregivers/{caregiverId}/unavailabilities", caregiverProfileId)
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reasonType": "SICK",
                                  "startsAt": "2026-05-01T12:00:00-05:00",
                                  "endsAt": "2026-05-01T18:00:00-05:00",
                                  "allDay": false,
                                  "approvalStatus": "PENDING",
                                  "notes": "Overlap"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/caregivers/{caregiverId}/performance-summary", caregiverProfileId)
                        .param("windowStart", "2026-04-01T00:00:00-05:00")
                        .param("windowEnd", "2026-05-31T23:59:59-05:00")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCredentialCount").value(1))
                .andExpect(jsonPath("$.activeLanguageCount").value(1))
                .andExpect(jsonPath("$.activeSkillCount").value(1))
                .andExpect(jsonPath("$.unsupportedMetrics[0]").value("COMPLETED_VISITS_COUNT"));

        mockMvc.perform(get("/api/audit-events")
                        .param("actionType", "WORKFORCE_PERFORMANCE_REFRESHED")
                        .with(authentication(TestTenantAuthentications.authenticationFor(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetType").value("CAREGIVER_PERFORMANCE_SUMMARY"));
    }

    @Test
    void caregiverRoleIsForbiddenFromWorkforceDirectoryAndMutations() throws Exception {
        Agency agency = persistAgency();
        AgencyMembership caregiver = persistMembership(agency, AgencyRole.CAREGIVER, "caregiver@northstar.example");

        mockMvc.perform(get("/api/caregivers")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/caregivers")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "agencyMembershipId": "%s",
                                  "caregiverCode": "CG-001",
                                  "displayName": "Forbidden"
                                }
                                """.formatted(caregiver.getId())))
                .andExpect(status().isForbidden());
    }

    private Agency persistAgency() {
        return agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
    }

    private AgencyMembership persistMembership(Agency agency, AgencyRole role, String email) {
        User user = userRepository.saveAndFlush(User.invite("Casey", "Care", email, "312-555-0101"));
        user.activateWithCredentials("{noop}test-password");
        userRepository.saveAndFlush(user);
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }
}
