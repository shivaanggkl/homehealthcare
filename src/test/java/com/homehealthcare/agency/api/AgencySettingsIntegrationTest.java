package com.homehealthcare.agency.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
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
class AgencySettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void ownerCanReadAndUpdateAgencySettingsWhileBranchAdminIsReadOnly() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        AgencyMembership branchAdminMembership = createMembership(createUser("Jordan", "Admin"), agency, AgencyRole.BRANCH_ADMIN);

        mockMvc.perform(get("/api/agency/settings")
                        .with(authentication(authenticationFor(branchAdminMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()))
                .andExpect(jsonPath("$.name").value("North Star Home Care"));

        mockMvc.perform(put("/api/agency/settings")
                        .with(authentication(authenticationFor(ownerMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "North Star Home Care Updated",
                                  "timezone": "America/New_York",
                                  "contactEmail": "support@northstar.example"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("North Star Home Care Updated"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"));

        assertThat(agencyRepository.findById(agency.getId()).orElseThrow().getContactEmail())
                .isEqualTo("support@northstar.example");
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(ownerMembership.getId()))
                .anySatisfy(event -> assertThat(event.getActionType()).isEqualTo("AGENCY_SETTINGS_UPDATED"));

        mockMvc.perform(put("/api/agency/settings")
                        .with(authentication(authenticationFor(branchAdminMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blocked",
                                  "timezone": "America/Chicago",
                                  "contactEmail": "blocked@northstar.example"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidAgencySettingsReturnBadRequest() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership ownerMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(put("/api/agency/settings")
                        .with(authentication(authenticationFor(ownerMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "North Star Home Care",
                                  "timezone": "Bad/Timezone",
                                  "contactEmail": "support@northstar.example"
                                }
                                """))
                .andExpect(status().isBadRequest());
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
