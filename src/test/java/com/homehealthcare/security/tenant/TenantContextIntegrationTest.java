package com.homehealthcare.security.tenant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TenantContextIntegrationTest.TestTenantController.class,
        TenantContextIntegrationTest.TestTenantService.class
})
class TenantContextIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resolvesAgencyContextForSingleMembershipAuthentication() throws Exception {
        UUID membershipId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        mockMvc.perform(get("/test/tenant-context")
                        .with(authentication(authenticationFor(new TestTenantPrincipal(
                                null,
                                Set.of(new TenantMembership(membershipId, agencyId)))))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(agencyId.toString()));
    }

    @Test
    void resolvesExplicitAgencySelectionWhenUserHasMultipleMemberships() throws Exception {
        UUID selectedAgencyId = UUID.randomUUID();
        UUID selectedMembershipId = UUID.randomUUID();

        mockMvc.perform(get("/test/tenant-context")
                        .with(authentication(authenticationFor(new TestTenantPrincipal(
                                selectedAgencyId,
                                Set.of(
                                        new TenantMembership(UUID.randomUUID(), UUID.randomUUID()),
                                        new TenantMembership(selectedMembershipId, selectedAgencyId)))))))
                .andExpect(status().isOk())
                .andExpect(content().string(selectedAgencyId.toString()));
    }

    @Test
    void rejectsCrossAgencyAccessAttempt() throws Exception {
        mockMvc.perform(get("/test/tenant-context")
                        .with(authentication(authenticationFor(new TestTenantPrincipal(
                                UUID.randomUUID(),
                                Set.of(new TenantMembership(UUID.randomUUID(), UUID.randomUUID())))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAmbiguousMembershipWithoutSelectedAgency() throws Exception {
        mockMvc.perform(get("/test/tenant-context")
                        .with(authentication(authenticationFor(new TestTenantPrincipal(
                                null,
                                Set.of(
                                        new TenantMembership(UUID.randomUUID(), UUID.randomUUID()),
                                        new TenantMembership(UUID.randomUUID(), UUID.randomUUID())))))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInactiveAgencyMemberships() throws Exception {
        mockMvc.perform(get("/test/tenant-context")
                        .with(authentication(authenticationFor(new TestTenantPrincipal(
                                null,
                                Set.of(new TenantMembership(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        AgencyMembershipStatus.INACTIVE)))))))
                .andExpect(status().isForbidden());
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(TestTenantPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "password",
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        authentication.setDetails(principal);
        return authentication;
    }

    record TestTenantPrincipal(UUID currentAgencyId, Set<TenantMembership> memberships) implements TenantAccessPrincipal {
    }

    @RestController
    static class TestTenantController {

        private final TestTenantService testTenantService;

        TestTenantController(TestTenantService testTenantService) {
            this.testTenantService = testTenantService;
        }

        @GetMapping(path = "/test/tenant-context", produces = MediaType.TEXT_PLAIN_VALUE)
        String currentAgencyId() {
            return testTenantService.currentAgencyId().toString();
        }
    }

    @Service
    static class TestTenantService {

        private final CurrentTenant currentTenant;

        TestTenantService(CurrentTenant currentTenant) {
            this.currentTenant = currentTenant;
        }

        UUID currentAgencyId() {
            return currentTenant.requireAgencyId();
        }
    }
}
