package com.homehealthcare.agencyprofile.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.agencyprofile.domain.AgencyProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
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
class AgencyProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AgencyProfileRepository agencyProfileRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void branchAdminCanReadAndUpdateAgencyProfileWhileCaregiverIsForbidden() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/agency/profile")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()))
                .andExpect(jsonPath("$.defaultTimezone").value("America/Chicago"));

        mockMvc.perform(put("/api/agency/profile")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "North Star Home Care",
                                  "legalName": "North Star Holdings LLC",
                                  "primaryPhone": "312-555-0101",
                                  "primaryAddress": "123 Main St",
                                  "operationsContactName": "Operations Lead",
                                  "operationsContactEmail": "operations@northstar.example",
                                  "supportContactName": "Support Desk",
                                  "supportContactEmail": "support@northstar.example",
                                  "defaultTimezone": "America/New_York",
                                  "defaultLocale": "en-US"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultTimezone").value("America/New_York"))
                .andExpect(jsonPath("$.operationsContactEmail").value("operations@northstar.example"));

        assertThat(agencyProfileRepository.findByAgency_Id(agency.getId())).isPresent();
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(branchAdmin.getId()))
                .anySatisfy(event -> assertThat(event.getTargetType()).isEqualTo("AGENCY_PROFILE"));

        mockMvc.perform(get("/api/agency/profile")
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
