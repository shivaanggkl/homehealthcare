package com.homehealthcare.user.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.TenantAccessPrincipal;
import com.homehealthcare.security.tenant.TenantMembership;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.time.OffsetDateTime;
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
class UserDirectoryApiIntegrationTest {

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
    void adminCanFilterDirectoryByStatusRoleAndBranch() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));

        User invitedUser = createUser("Casey", "Caregiver");
        AgencyMembership invitedMembership = createMembership(invitedUser, agency, AgencyRole.CAREGIVER);
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(invitedMembership, branch));

        User activeUser = createUser("Jordan", "Caregiver");
        activeUser.activate();
        activeUser.recordLogin(OffsetDateTime.now());
        userRepository.saveAndFlush(activeUser);
        createMembership(activeUser, agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/users")
                        .param("status", UserStatus.INVITED.name())
                        .param("role", AgencyRole.CAREGIVER.name())
                        .param("branchId", branch.getId().toString())
                        .param("page", "0")
                        .param("size", "10")
                        .with(authentication(authenticationFor(actorMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(invitedUser.getId().toString()))
                .andExpect(jsonPath("$.content[0].role").value("CAREGIVER"))
                .andExpect(jsonPath("$.content[0].userStatus").value("INVITED"))
                .andExpect(jsonPath("$.content[0].branchNames[0]").value("Austin Branch"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void nonAdminCannotAccessDirectory() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiverMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/users")
                        .with(authentication(authenticationFor(caregiverMembership))))
                .andExpect(status().isForbidden());
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
