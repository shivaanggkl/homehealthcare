package com.homehealthcare.branch.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.TenantAccessPrincipal;
import com.homehealthcare.security.tenant.TenantMembership;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BranchManagementIntegrationTest {

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

    @Test
    void ownerCanListCreateUpdateAndDeactivateBranches() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        Branch existingBranch = branchRepository.saveAndFlush(
                Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));

        mockMvc.perform(get("/api/branches")
                        .param("search", "aus")
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Austin Branch"));

        mockMvc.perform(post("/api/branches")
                        .with(authentication(authenticationFor(ownerMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "agencyId": "%s",
                                  "name": "Dallas Branch",
                                  "code": "DAL",
                                  "address": "500 Elm",
                                  "timezone": "America/Chicago"
                                }
                                """.formatted(agency.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dallas Branch"))
                .andExpect(jsonPath("$.code").value("DAL"));

        Branch created = branchRepository.findByAgency_IdAndCode(agency.getId(), "DAL").orElseThrow();

        mockMvc.perform(put("/api/branches/{branchId}", created.getId())
                        .with(authentication(authenticationFor(ownerMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dallas Updated",
                                  "code": "DAL-2",
                                  "address": "510 Elm",
                                  "timezone": "America/New_York"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dallas Updated"))
                .andExpect(jsonPath("$.code").value("DAL-2"));

        mockMvc.perform(delete("/api/branches/{branchId}", existingBranch.getId())
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void caregiverCannotAccessBranchManagementAndBranchAdminCannotDeactivate() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiverMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        AgencyMembership branchAdminMembership = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        Branch branch = branchRepository.saveAndFlush(
                Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));

        mockMvc.perform(get("/api/branches")
                        .with(authentication(authenticationFor(caregiverMembership))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/branches/{branchId}", branch.getId())
                        .with(authentication(authenticationFor(branchAdminMembership))))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateBranchCodeReturnsConflict() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        branchRepository.saveAndFlush(Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));

        mockMvc.perform(post("/api/branches")
                        .with(authentication(authenticationFor(ownerMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "agencyId": "%s",
                                  "name": "Dallas Branch",
                                  "code": "ATX",
                                  "address": "500 Elm",
                                  "timezone": "America/Chicago"
                                }
                                """.formatted(agency.getId())))
                .andExpect(status().isConflict());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(firstName, lastName, UUID.randomUUID() + "@northstar.example", null));
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(AgencyMembership membership) {
        TestTenantPrincipal principal = new TestTenantPrincipal(
                membership.getAgencyId(),
                Set.of(new TenantMembership(membership.getId(), membership.getAgencyId())));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        authentication.setDetails(principal);
        return authentication;
    }

    record TestTenantPrincipal(UUID currentAgencyId, Set<TenantMembership> memberships) implements TenantAccessPrincipal {
    }
}
