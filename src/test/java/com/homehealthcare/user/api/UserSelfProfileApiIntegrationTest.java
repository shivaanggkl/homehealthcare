package com.homehealthcare.user.api;

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
class UserSelfProfileApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Test
    void userCanReadAndUpdateOwnProfile() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User user = userRepository.saveAndFlush(User.invite("Casey", "Caregiver", UUID.randomUUID() + "@northstar.example", "555-111-2222"));
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.CAREGIVER));

        mockMvc.perform(get("/api/me/profile")
                        .with(authentication(authenticationFor(membership))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));

        mockMvc.perform(put("/api/me/profile")
                        .with(authentication(authenticationFor(membership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey Updated",
                                  "lastName": "Caregiver Updated",
                                  "phone": "555-333-4444",
                                  "preferredLanguage": "en-US",
                                  "timeZone": "America/New_York"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Casey Updated"))
                .andExpect(jsonPath("$.preferredLanguage").value("en-US"))
                .andExpect(jsonPath("$.timeZone").value("America/New_York"));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getPreferredLanguage()).isEqualTo("en-US");
        assertThat(savedUser.getTimeZone()).isEqualTo("America/New_York");
    }

    @Test
    void invalidSelfProfileValuesReturnBadRequest() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User user = userRepository.saveAndFlush(User.invite("Casey", "Caregiver", UUID.randomUUID() + "@northstar.example", null));
        AgencyMembership membership = agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.CAREGIVER));

        mockMvc.perform(put("/api/me/profile")
                        .with(authentication(authenticationFor(membership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "phone": null,
                                  "preferredLanguage": "english",
                                  "timeZone": "Bad/Timezone"
                                }
                                """))
                .andExpect(status().isBadRequest());
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
