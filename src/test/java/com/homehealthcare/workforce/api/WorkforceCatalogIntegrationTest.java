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
class WorkforceCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private CaregiverSkillRepository caregiverSkillRepository;

    @Autowired
    private CaregiverCertificationRepository caregiverCertificationRepository;

    @Test
    void branchAdminCanCrudSkillsAndCertificationsWhileCaregiverIsForbidden() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        CaregiverSkill skill = caregiverSkillRepository.saveAndFlush(
                CaregiverSkill.create(agency, "Wound Care", "WC", "Wound care"));
        CaregiverCertification certification = caregiverCertificationRepository.saveAndFlush(
                CaregiverCertification.create(agency, "CPR", "CPR", "CPR certification", true));

        mockMvc.perform(get("/api/caregiver-skills")
                        .param("search", "wound")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Wound Care"));

        mockMvc.perform(post("/api/caregiver-skills")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dementia Care",
                                  "code": "DEM",
                                  "description": "Memory support"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DEM"));

        CaregiverSkill createdSkill = caregiverSkillRepository.findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agency.getId())
                .stream()
                .filter(item -> item.getCode().equals("DEM"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(put("/api/caregiver-skills/{skillId}", createdSkill.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dementia Care Updated",
                                  "code": "DEM-2",
                                  "description": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dementia Care Updated"));

        mockMvc.perform(delete("/api/caregiver-skills/{skillId}", skill.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/caregiver-certifications")
                        .param("search", "cpr")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("CPR"));

        mockMvc.perform(post("/api/caregiver-certifications")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "HHA",
                                  "code": "HHA",
                                  "description": "Home health aide",
                                  "expirationRequired": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expirationRequired").value(false));

        CaregiverCertification createdCertification = caregiverCertificationRepository
                .findAllByAgency_IdOrderByDisplayOrderAscNameAsc(agency.getId())
                .stream()
                .filter(item -> item.getCode().equals("HHA"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(put("/api/caregiver-certifications/{certificationId}", createdCertification.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "HHA Updated",
                                  "code": "HHA-2",
                                  "description": "Updated",
                                  "expirationRequired": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expirationRequired").value(true));

        mockMvc.perform(delete("/api/caregiver-certifications/{certificationId}", certification.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/caregiver-skills")
                        .with(authentication(TestTenantAuthentications.authenticationFor(caregiver))))
                .andExpect(status().isForbidden());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }
}
