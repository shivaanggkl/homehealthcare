package com.homehealthcare.branchpolicy.api;

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
import com.homehealthcare.branchpolicy.domain.BranchPolicy;
import com.homehealthcare.branchpolicy.domain.BranchPolicyRepository;
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
class BranchPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;
    @Autowired
    private BranchRepository branchRepository;
    @Autowired
    private BranchPolicyRepository branchPolicyRepository;

    @Test
    void branchAdminCanCrudBranchPoliciesAndFallbackIsExposed() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago", "CHI", "123 Main", "America/Chicago"));
        BranchPolicy existing = branchPolicyRepository.saveAndFlush(
                BranchPolicy.create(branch, "scheduling.window", "{\"minutes\":30}", false, 1));

        mockMvc.perform(get("/api/branch-policies")
                        .param("branchId", branch.getId().toString())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].branchId").value(branch.getId().toString()));

        mockMvc.perform(post("/api/branch-policies")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "policyKey": "care.plan.default",
                                  "settingsPayloadJson": null,
                                  "fallbackToAgencyDefault": true,
                                  "displayOrder": 2
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usesAgencyDefault").value(true))
                .andExpect(jsonPath("$.effectiveSettingsPayloadJson").doesNotExist());

        mockMvc.perform(put("/api/branch-policies/{branchPolicyId}", existing.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "policyKey": "scheduling.window",
                                  "settingsPayloadJson": "{\\\"minutes\\\":45}",
                                  "fallbackToAgencyDefault": false,
                                  "displayOrder": 1
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveSettingsPayloadJson").value("{\"minutes\":45}"));

        mockMvc.perform(delete("/api/branch-policies/{branchPolicyId}", existing.getId())
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void crossAgencyBranchAccessIsDeniedAsNotFound() throws Exception {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", UUID.randomUUID().toString(), "America/New_York", "ops@sunrise.example"));
        AgencyMembership branchAdmin = createMembership(createUser("Jordan", "Admin"), agencyOne, AgencyRole.BRANCH_ADMIN);
        Branch foreignBranch = branchRepository.saveAndFlush(
                Branch.create(agencyTwo, "Brooklyn", "BK", "500 Atlantic", "America/New_York"));

        mockMvc.perform(post("/api/branch-policies")
                        .with(authentication(TestTenantAuthentications.authenticationFor(branchAdmin)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "branchId": "%s",
                                  "policyKey": "scheduling.window",
                                  "settingsPayloadJson": "{\\\"minutes\\\":30}",
                                  "fallbackToAgencyDefault": false,
                                  "displayOrder": 1
                                }
                                """.formatted(foreignBranch.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void caregiverIsForbiddenForBranchPolicyApis() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiver = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/branch-policies")
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
