package com.homehealthcare.branch.application;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.homehealthcare.security.branch.BranchAccessPrincipal;
import com.homehealthcare.security.tenant.TenantAccessPrincipal;
import com.homehealthcare.security.tenant.TenantMembership;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(BranchTenantIsolationIntegrationTest.TestBranchController.class)
class BranchTenantIsolationIntegrationTest {

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
    private BranchAssignmentRepository branchAssignmentRepository;

    @Test
    void authenticatedTenantCannotEnumerateBranchIdsFromAnotherAgency() throws Exception {
        Agency agencyOne = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Agency agencyTwo = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care", "America/New_York", "ops@sunrise.example"));
        Branch agencyOneBranch = branchRepository.saveAndFlush(
                Branch.create(agencyOne, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch agencyTwoBranch = branchRepository.saveAndFlush(
                Branch.create(agencyTwo, "Brooklyn Central", "BK-01", "456 Flatbush Ave", "America/New_York"));

        mockMvc.perform(get("/test/branches/{branchId}", agencyOneBranch.getId())
                        .with(authentication(authenticationFor(agencyOne.getId()))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Chicago Central"));

        mockMvc.perform(get("/test/branches/{branchId}", agencyTwoBranch.getId())
                        .with(authentication(authenticationFor(agencyOne.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void branchScopedRoleOnlySeesAssignedBranchesAndSupportsMultipleAssignments() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        Branch first = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch second = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-02", "456 Lake St", "America/Chicago"));
        Branch third = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago South", "CHI-03", "789 State St", "America/Chicago"));

        mockMvc.perform(get("/test/branches")
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.SCHEDULER_COORDINATOR,
                                Set.of(first.getId(), third.getId())))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Chicago Central|Chicago South"));

        mockMvc.perform(get("/test/branches/{branchId}", second.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.SCHEDULER_COORDINATOR,
                                Set.of(first.getId(), third.getId())))))
                .andExpect(status().isNotFound());
    }

    @Test
    void agencyOwnerCanSeeAllAgencyBranchesWithoutExplicitAssignments() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-02", "456 Lake St", "America/Chicago"));

        mockMvc.perform(get("/test/branches")
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.AGENCY_OWNER,
                                Set.of()))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Chicago Central|Chicago North"));
    }

    @Test
    void resolvesPersistedBranchAssignmentsForApiVisibilityAndDeniesUnassignedBranches() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        User user = userRepository.saveAndFlush(
                User.invite("Casey", "Scheduler", "casey.scheduler@northstar.example", "+1 312 555 0101"));
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(user, agency, AgencyRole.SCHEDULER_COORDINATOR));
        Branch first = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch second = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-02", "456 Lake St", "America/Chicago"));
        Branch third = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago South", "CHI-03", "789 State St", "America/Chicago"));

        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(membership, first));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(membership, third));

        mockMvc.perform(get("/test/branches")
                        .with(authentication(tenantOnlyAuthenticationFor(membership))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Chicago Central|Chicago South"));

        mockMvc.perform(get("/test/branches/{branchId}", second.getId())
                        .with(authentication(tenantOnlyAuthenticationFor(membership))))
                .andExpect(status().isNotFound());
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(UUID agencyId) {
        return authenticationFor(agencyId, AgencyRole.AGENCY_OWNER, Set.of());
    }

    private static UsernamePasswordAuthenticationToken tenantOnlyAuthenticationFor(AgencyMembership membership) {
        TenantOnlyPrincipal principal = new TenantOnlyPrincipal(
                membership.getAgencyId(),
                Set.of(new TenantMembership(membership.getId(), membership.getAgencyId())));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        authentication.setDetails(principal);
        return authentication;
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(
            UUID agencyId,
            AgencyRole agencyRole,
            Set<UUID> assignedBranchIds) {
        TestTenantPrincipal principal = new TestTenantPrincipal(
                agencyId,
                Set.of(new TenantMembership(UUID.randomUUID(), agencyId)),
                agencyRole,
                assignedBranchIds);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        authentication.setDetails(principal);
        return authentication;
    }

    record TestTenantPrincipal(
            UUID currentAgencyId,
            Set<TenantMembership> memberships,
            AgencyRole agencyRole,
            Set<UUID> assignedBranchIds) implements TenantAccessPrincipal, BranchAccessPrincipal {
    }

    record TenantOnlyPrincipal(UUID currentAgencyId, Set<TenantMembership> memberships) implements TenantAccessPrincipal {
    }

    @RestController
    static class TestBranchController {

        private final BranchService branchService;

        TestBranchController(BranchService branchService) {
            this.branchService = branchService;
        }

        @GetMapping(path = "/test/branches/{branchId}", produces = MediaType.TEXT_PLAIN_VALUE)
        String branchName(@PathVariable UUID branchId) {
            return branchService.getBranchForCurrentAgency(branchId).getName();
        }

        @GetMapping(path = "/test/branches", produces = MediaType.TEXT_PLAIN_VALUE)
        String visibleBranches(@RequestParam(required = false) String unused) {
            List<String> branchNames = branchService.listAccessibleBranchesForCurrentAgency().stream()
                    .map(Branch::getName)
                    .toList();
            return String.join("|", branchNames);
        }
    }
}
