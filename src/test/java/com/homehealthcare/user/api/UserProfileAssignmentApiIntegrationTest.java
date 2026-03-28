package com.homehealthcare.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
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
class UserProfileAssignmentApiIntegrationTest {

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
    private BranchAssignmentRepository branchAssignmentRepository;

    @Test
    void adminCanUpdateUserProfileRoleAndBranchAssignments() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        User targetUser = createUser("Casey", "Caregiver");
        AgencyMembership targetMembership = createMembership(targetUser, agency, AgencyRole.CAREGIVER);
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));

        mockMvc.perform(put("/api/users/{userId}", targetUser.getId())
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey Updated",
                                  "lastName": "Caregiver Updated",
                                  "phone": "555-444-1212",
                                  "role": "BRANCH_ADMIN",
                                  "branchIds": ["%s"]
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(targetUser.getId().toString()))
                .andExpect(jsonPath("$.membershipId").value(targetMembership.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Casey Updated"))
                .andExpect(jsonPath("$.role").value("BRANCH_ADMIN"))
                .andExpect(jsonPath("$.branchIds[0]").value(branch.getId().toString()))
                .andExpect(jsonPath("$.branchNames[0]").value("Austin Branch"));

        AgencyMembership reloadedMembership = agencyMembershipRepository.findById(targetMembership.getId()).orElseThrow();
        assertThat(reloadedMembership.getRole()).isEqualTo(AgencyRole.BRANCH_ADMIN);
        assertThat(reloadedMembership.getUser().getPhone()).isEqualTo("555-444-1212");
        assertThat(branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                targetMembership.getId(),
                BranchAssignmentStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void branchAdminCannotAssignAgencyOwnerRole() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);
        User targetUser = createUser("Casey", "Caregiver");
        createMembership(targetUser, agency, AgencyRole.CAREGIVER);

        mockMvc.perform(put("/api/users/{userId}", targetUser.getId())
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "phone": null,
                                  "role": "AGENCY_OWNER",
                                  "branchIds": []
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void caregiverCannotEditAnotherUser() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        User targetUser = createUser("Jordan", "Staff");
        createMembership(targetUser, agency, AgencyRole.CAREGIVER);

        mockMvc.perform(put("/api/users/{userId}", targetUser.getId())
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Jordan",
                                  "lastName": "Staff",
                                  "phone": null,
                                  "role": "CAREGIVER",
                                  "branchIds": []
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossAgencyTargetUserReturnsNotFound() throws Exception {
        Agency actorAgency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), actorAgency, AgencyRole.AGENCY_OWNER);

        Agency otherAgency = agencyRepository.saveAndFlush(
                Agency.create("Southwind Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@southwind.example"));
        User targetUser = createUser("Jordan", "Elsewhere");
        createMembership(targetUser, otherAgency, AgencyRole.CAREGIVER);

        mockMvc.perform(put("/api/users/{userId}", targetUser.getId())
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Jordan",
                                  "lastName": "Elsewhere",
                                  "phone": null,
                                  "role": "CAREGIVER",
                                  "branchIds": []
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createUser(String firstName, String lastName) {
        return userRepository.saveAndFlush(User.invite(
                firstName,
                lastName,
                UUID.randomUUID() + "@northstar.example",
                null));
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
