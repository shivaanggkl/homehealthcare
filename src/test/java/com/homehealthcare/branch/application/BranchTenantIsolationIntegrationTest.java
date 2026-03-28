package com.homehealthcare.branch.application;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
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
import org.springframework.web.bind.annotation.PostMapping;
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

    @Autowired
    private AuditEventRepository auditEventRepository;

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
    void authenticatedTenantCannotCreateBranchInAnotherAgency() throws Exception {
        Agency actorAgency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care-write", "America/Chicago", "ops@northstar.example"));
        Agency otherAgency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", "sunrise-home-care-write", "America/New_York", "ops@sunrise.example"));

        mockMvc.perform(post("/test/branches/create")
                        .with(authentication(authenticationFor(actorAgency.getId())))
                        .param("agencyId", otherAgency.getId().toString())
                        .param("name", "Unauthorized Branch")
                        .param("code", "UNAUTH-01")
                        .param("address", "999 Forbidden Ave")
                        .param("timezone", "America/New_York"))
                .andExpect(status().isForbidden());

        assertThat(branchRepository.findByAgency_IdAndCode(otherAgency.getId(), "UNAUTH-01")).isEmpty();
    }

    @Test
    void branchCreateAndDeactivateAreAuditedForAuthorizedAgencyActor() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care-audit-branch", "America/Chicago", "ops@northstar.example"));
        User actorUser = userRepository.saveAndFlush(
                User.invite("Alicia", "Owner", "alicia.branch.audit@northstar.example", null));
        AgencyMembership actorMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(actorUser, agency, AgencyRole.AGENCY_OWNER));

        mockMvc.perform(post("/test/branches/create")
                        .with(authentication(tenantOnlyAuthenticationFor(actorMembership)))
                        .param("agencyId", agency.getId().toString())
                        .param("name", "Audit Branch")
                        .param("code", "AUD-01")
                        .param("address", "500 Audit St")
                        .param("timezone", "America/Chicago"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Audit Branch"));

        Branch createdBranch = branchRepository.findByAgency_IdAndCode(agency.getId(), "AUD-01").orElseThrow();

        mockMvc.perform(post("/test/branches/{branchId}/deactivate", createdBranch.getId())
                        .with(authentication(tenantOnlyAuthenticationFor(actorMembership))))
                .andExpect(status().isOk())
                .andExpect(content().string("INACTIVE"));

        List<AuditEvent> auditEvents = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(actorMembership.getId());
        assertThat(auditEvents)
                .extracting(AuditEvent::getActionType)
                .contains("BRANCH_CREATED", "BRANCH_DEACTIVATED");
        assertThat(auditEvents)
                .filteredOn(event -> event.getActionType().equals("BRANCH_CREATED"))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAgencyId()).isEqualTo(agency.getId());
                    assertThat(event.getBranchId()).isEqualTo(createdBranch.getId());
                });
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
    void singleBranchSchedulerCannotViewOrEditOtherBranch() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care-single", "America/Chicago", "ops@northstar.example"));
        Branch branchA = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-11", "123 Main St", "America/Chicago"));
        Branch branchB = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-12", "456 Lake St", "America/Chicago"));

        mockMvc.perform(get("/test/branches/{branchId}", branchA.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.SCHEDULER_COORDINATOR,
                                Set.of(branchA.getId())))))
                .andExpect(status().isOk())
                .andExpect(content().string("Chicago Central"));

        mockMvc.perform(get("/test/branches/{branchId}", branchB.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.SCHEDULER_COORDINATOR,
                                Set.of(branchA.getId())))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/test/branches/{branchId}/deactivate", branchB.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.SCHEDULER_COORDINATOR,
                                Set.of(branchA.getId())))))
                .andExpect(status().isForbidden());

        assertThat(branchRepository.findById(branchB.getId()).orElseThrow().isDeactivated()).isFalse();
    }

    @Test
    void agencyOwnerCanSeeAllAgencyBranchesWithoutExplicitAssignments() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care", "America/Chicago", "ops@northstar.example"));
        branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-01", "123 Main St", "America/Chicago"));
        Branch second = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-02", "456 Lake St", "America/Chicago"));

        mockMvc.perform(get("/test/branches")
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.AGENCY_OWNER,
                                Set.of()))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Chicago Central|Chicago North"));

        mockMvc.perform(post("/test/branches/{branchId}/deactivate", second.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.AGENCY_OWNER,
                                Set.of()))))
                .andExpect(status().isOk())
                .andExpect(content().string("INACTIVE"));

        assertThat(branchRepository.findById(second.getId()).orElseThrow().isDeactivated()).isTrue();
    }

    @Test
    void agencyWideReadRoleCanViewAllBranchesWithoutExplicitAssignments() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care-audit", "America/Chicago", "ops@northstar.example"));
        Branch first = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-31", "123 Main St", "America/Chicago"));
        Branch second = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-32", "456 Lake St", "America/Chicago"));

        mockMvc.perform(get("/test/branches")
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.READ_ONLY_AUDITOR,
                                Set.of()))))
                .andExpect(status().isOk())
                .andExpect(content().string("Chicago Central|Chicago North"));

        mockMvc.perform(get("/test/branches/{branchId}", second.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.READ_ONLY_AUDITOR,
                                Set.of()))))
                .andExpect(status().isOk())
                .andExpect(content().string("Chicago North"));

        assertThat(branchRepository.findById(first.getId()).orElseThrow().isDeactivated()).isFalse();
    }

    @Test
    void multiBranchBranchAdminCanEditAssignedBranchButNotUnassignedBranch() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", "north-star-home-care-admin", "America/Chicago", "ops@northstar.example"));
        Branch first = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago Central", "CHI-21", "123 Main St", "America/Chicago"));
        Branch second = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago North", "CHI-22", "456 Lake St", "America/Chicago"));
        Branch third = branchRepository.saveAndFlush(
                Branch.create(agency, "Chicago South", "CHI-23", "789 State St", "America/Chicago"));

        mockMvc.perform(post("/test/branches/{branchId}/deactivate", third.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.BRANCH_ADMIN,
                                Set.of(first.getId(), third.getId())))))
                .andExpect(status().isOk())
                .andExpect(content().string("INACTIVE"));

        mockMvc.perform(post("/test/branches/{branchId}/deactivate", second.getId())
                        .with(authentication(authenticationFor(
                                agency.getId(),
                                AgencyRole.BRANCH_ADMIN,
                                Set.of(first.getId(), third.getId())))))
                .andExpect(status().isForbidden());

        assertThat(branchRepository.findById(third.getId()).orElseThrow().isDeactivated()).isTrue();
        assertThat(branchRepository.findById(second.getId()).orElseThrow().isDeactivated()).isFalse();
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

        @PostMapping(path = "/test/branches/create", produces = MediaType.TEXT_PLAIN_VALUE)
        String createBranch(
                @RequestParam UUID agencyId,
                @RequestParam String name,
                @RequestParam String code,
                @RequestParam String address,
                @RequestParam String timezone) {
            return branchService.createBranch(new BranchService.CreateBranchCommand(
                    agencyId,
                    name,
                    code,
                    address,
                    timezone)).getName();
        }

        @PostMapping(path = "/test/branches/{branchId}/deactivate", produces = MediaType.TEXT_PLAIN_VALUE)
        String deactivateBranch(@PathVariable UUID branchId) {
            return branchService.deactivateBranchForCurrentAgency(branchId).getStatus().name();
        }
    }
}
