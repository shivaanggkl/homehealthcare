package com.homehealthcare.notification.api;

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
import com.homehealthcare.notification.domain.AdminNotificationPreferenceRepository;
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
class AdminNotificationPreferencesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private AdminNotificationPreferenceRepository adminNotificationPreferenceRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void adminCanViewAndUpdateNotificationPreferences() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createActiveUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(get("/api/security/admin-notifications")
                        .with(authentication(authenticationFor(actorMembership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipId").value(actorMembership.getId().toString()))
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.failedLoginAlertsEnabled").value(true))
                .andExpect(jsonPath("$.lockedAccountAlertsEnabled").value(true))
                .andExpect(jsonPath("$.newAdminAlertsEnabled").value(true));

        mockMvc.perform(put("/api/security/admin-notifications")
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "emailEnabled": true,
                                  "failedLoginAlertsEnabled": false,
                                  "lockedAccountAlertsEnabled": true,
                                  "newAdminAlertsEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failedLoginAlertsEnabled").value(false))
                .andExpect(jsonPath("$.newAdminAlertsEnabled").value(false));

        assertThat(adminNotificationPreferenceRepository.findByAgencyMembership_Id(actorMembership.getId())).isPresent()
                .get()
                .satisfies(preference -> {
                    assertThat(preference.isFailedLoginAlertsEnabled()).isFalse();
                    assertThat(preference.isNewAdminAlertsEnabled()).isFalse();
                });
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(actorMembership.getId()))
                .anySatisfy(event -> assertThat(event.getActionType()).isEqualTo("ADMIN_NOTIFICATION_PREFERENCES_UPDATED"));
    }

    @Test
    void nonAdminCannotManageNotificationPreferences() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("Sunrise Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@sunrise.example"));
        AgencyMembership caregiverMembership = createMembership(createActiveUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(get("/api/security/admin-notifications")
                        .with(authentication(authenticationFor(caregiverMembership))))
                .andExpect(status().isForbidden());
    }

    private AgencyMembership createMembership(User user, Agency agency, AgencyRole role) {
        return agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, role));
    }

    private User createActiveUser(String firstName, String lastName) {
        User user = userRepository.saveAndFlush(User.invite(
                firstName,
                lastName,
                UUID.randomUUID() + "@northstar.example",
                null));
        user.activate();
        return userRepository.saveAndFlush(user);
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
