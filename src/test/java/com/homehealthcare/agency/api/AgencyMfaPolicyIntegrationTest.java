package com.homehealthcare.agency.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyMfaPolicyMode;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
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
class AgencyMfaPolicyIntegrationTest {

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
    void adminCanViewAndUpdateAgencyMfaPolicyWithoutSeeingSecrets() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(get("/api/security/mfa-policy")
                        .with(authentication(authenticationFor(actorMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()))
                .andExpect(jsonPath("$.mode").value("OFF"))
                .andExpect(jsonPath("$.requiredRoles").isArray())
                .andExpect(jsonPath("$.requiredRoles").isEmpty())
                .andExpect(jsonPath("$.secret").doesNotExist())
                .andExpect(jsonPath("$.mfaSecret").doesNotExist());

        mockMvc.perform(put("/api/security/mfa-policy")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "SELECTED_ROLES",
                                  "requiredRoles": ["BRANCH_ADMIN", "CAREGIVER"]
                                }
                                """)
                        .with(authentication(authenticationFor(actorMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("SELECTED_ROLES"))
                .andExpect(jsonPath("$.requiredRoles").isArray())
                .andExpect(jsonPath("$.requiredRoles.length()").value(2))
                .andExpect(jsonPath("$.mfaSecret").doesNotExist());

        Agency savedAgency = agencyRepository.findById(agency.getId()).orElseThrow();
        assertThat(savedAgency.getMfaPolicyMode()).isEqualTo(AgencyMfaPolicyMode.SELECTED_ROLES);
        assertThat(savedAgency.requiredMfaRoles()).containsExactly(AgencyRole.BRANCH_ADMIN, AgencyRole.CAREGIVER);

        AuditEvent auditEvent = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(actorMembership.getId())
                .stream()
                .filter(event -> event.getActionType().equals("AGENCY_MFA_POLICY_UPDATED"))
                .findFirst()
                .orElseThrow();
        assertThat(auditEvent.getAgencyId()).isEqualTo(agency.getId());
        assertThat(auditEvent.getMetadataJson()).contains("\"mode\":\"SELECTED_ROLES\"");
        assertThat(auditEvent.getMetadataJson()).contains("BRANCH_ADMIN");
    }

    @Test
    void nonAdminCannotManageAgencyMfaPolicyAndInvalidRoleModeCombinationFails() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiverMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);
        AgencyMembership ownerMembership = createMembership(createUser("Jordan", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(get("/api/security/mfa-policy")
                        .with(authentication(authenticationFor(caregiverMembership))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/security/mfa-policy")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "ALL_USERS",
                                  "requiredRoles": ["CAREGIVER"]
                                }
                                """)
                        .with(authentication(authenticationFor(ownerMembership))))
                .andExpect(status().isBadRequest());
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
