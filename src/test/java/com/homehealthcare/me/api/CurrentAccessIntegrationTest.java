package com.homehealthcare.me.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CurrentAccessIntegrationTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void returnsCurrentRolePermissionsAndAssignedBranches() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User user = userRepository.saveAndFlush(User.invite("Casey", "Scheduler", UUID.randomUUID() + "@northstar.example", null));
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.SCHEDULER_COORDINATOR));
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));
        branchAssignmentRepository.saveAndFlush(BranchAssignment.assign(membership, branch));

        mockMvc.perform(get("/api/me/access")
                        .with(authentication(authenticationFor(membership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()))
                .andExpect(jsonPath("$.membershipId").value(membership.getId().toString()))
                .andExpect(jsonPath("$.role").value("SCHEDULER_COORDINATOR"))
                .andExpect(jsonPath("$.branchScope").value("ASSIGNED_BRANCHES"))
                .andExpect(jsonPath("$.assignedBranchIds[0]").value(branch.getId().toString()))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("VIEW_BRANCH")));
    }

    @Test
    void returnsAgencyWideScopeForAgencyOwner() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("Evergreen Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@evergreen.example"));
        User owner = userRepository.saveAndFlush(User.invite("Alicia", "Owner", UUID.randomUUID() + "@evergreen.example", null));
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(owner, agency, AgencyRole.AGENCY_OWNER));

        mockMvc.perform(get("/api/me/access")
                        .with(authentication(authenticationFor(membership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENCY_OWNER"))
                .andExpect(jsonPath("$.branchScope").value("AGENCY_WIDE"))
                .andExpect(jsonPath("$.assignedBranchIds").isArray())
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("VIEW_USER_DIRECTORY")))
                .andExpect(jsonPath("$.permissions").value(org.hamcrest.Matchers.hasItem("INVITE_USER")));
    }

    @Test
    void returnsCurrentAccessForAuthenticatedSessionTokenFlow() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User owner = userRepository.saveAndFlush(User.invite("Alicia", "Owner", "alicia.owner+" + UUID.randomUUID() + "@northstar.example", null));
        owner.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(owner);
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(owner, agency, AgencyRole.AGENCY_OWNER));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StartPassword1!"
                                }
                                """.formatted(owner.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = extractJsonString(loginResponse, "accessToken");
        String sessionId = extractJsonString(loginResponse, "sessionId");

        mockMvc.perform(get("/api/me/access")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Session-Id", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.getId().toString()))
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()))
                .andExpect(jsonPath("$.membershipId").value(membership.getId().toString()))
                .andExpect(jsonPath("$.role").value("AGENCY_OWNER"))
                .andExpect(jsonPath("$.branchScope").value("AGENCY_WIDE"));
    }

    private static String extractJsonString(String json, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("Could not extract field '" + fieldName + "' from payload: " + json);
        }
        return matcher.group(1);
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
