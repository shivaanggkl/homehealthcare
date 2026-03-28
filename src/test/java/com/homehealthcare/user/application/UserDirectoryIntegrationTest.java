package com.homehealthcare.user.application;

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
class UserDirectoryIntegrationTest {

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
    void adminCanSearchFilterAndPaginateUserDirectory() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        Branch northBranch = branchRepository.saveAndFlush(
                Branch.create(agency, "North Branch", "NB-01", "123 North St", "America/Chicago"));
        Branch southBranch = branchRepository.saveAndFlush(
                Branch.create(agency, "South Branch", "SB-01", "456 South St", "America/Chicago"));

        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        User casey = createUser("Casey", "Scheduler");
        casey.activateWithCredentials("hash");
        casey.recordLogin(OffsetDateTime.now().minusHours(2));
        casey.enableMfa();
        userRepository.saveAndFlush(casey);
        AgencyMembership caseyMembership = createMembership(casey, agency, AgencyRole.SCHEDULER_COORDINATOR);
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(caseyMembership, northBranch));

        User jordan = createUser("Jordan", "Reviewer");
        jordan.activateWithCredentials("hash");
        userRepository.saveAndFlush(jordan);
        AgencyMembership jordanMembership = createMembership(jordan, agency, AgencyRole.QA_CLINICAL_REVIEWER);
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(jordanMembership, southBranch));

        mockMvc.perform(get("/api/users")
                        .param("status", UserStatus.ACTIVE.name())
                        .param("role", AgencyRole.SCHEDULER_COORDINATOR.name())
                        .param("branchId", northBranch.getId().toString())
                        .param("search", "casey")
                        .param("page", "0")
                        .param("size", "1")
                        .with(authentication(authenticationFor(actorMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(casey.getEmail()))
                .andExpect(jsonPath("$.content[0].mfaEnabled").value(true))
                .andExpect(jsonPath("$.content[0].mfaSecret").doesNotExist())
                .andExpect(jsonPath("$.content[0].userStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].lastLoginAt").exists())
                .andExpect(jsonPath("$.content[0].branchNames[0]").value("North Branch"));
    }

    @Test
    void onlyPermittedAdminsCanAccessDirectory() throws Exception {
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
