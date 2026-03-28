package com.homehealthcare.invitation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.invitation.domain.UserInvitation;
import com.homehealthcare.invitation.domain.UserInvitationRepository;
import com.homehealthcare.invitation.domain.UserInvitationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.TenantAccessPrincipal;
import com.homehealthcare.security.tenant.TenantMembership;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
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
class UserInvitationApiIntegrationTest {

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
    private UserInvitationRepository userInvitationRepository;

    @Test
    void adminCanInviteInspectAndAcceptInvitation() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        Branch branch = branchRepository.saveAndFlush(Branch.create(agency, "Austin Branch", "ATX", "100 Main", "America/Chicago"));

        mockMvc.perform(post("/api/users/invitations")
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "email": "casey.caregiver@example.com",
                                  "phone": "555-000-1234",
                                  "role": "CAREGIVER",
                                  "branchIds": ["%s"]
                                }
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()))
                .andExpect(jsonPath("$.email").value("casey.caregiver@example.com"))
                .andExpect(jsonPath("$.role").value("CAREGIVER"))
                .andExpect(jsonPath("$.branchIds[0]").value(branch.getId().toString()))
                .andExpect(jsonPath("$.branchNames[0]").value("Austin Branch"));

        UserInvitation invitation = userInvitationRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/invitations/{token}", invitation.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationId").value(invitation.getId().toString()))
                .andExpect(jsonPath("$.email").value("casey.caregiver@example.com"))
                .andExpect(jsonPath("$.role").value("CAREGIVER"))
                .andExpect(jsonPath("$.branchNames[0]").value("Austin Branch"));

        mockMvc.perform(post("/api/invitations/{token}/accept", invitation.getToken())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "phone": "555-000-9999",
                                  "password": "ValidPassword!123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationId").value(invitation.getId().toString()))
                .andExpect(jsonPath("$.agencyId").value(agency.getId().toString()));

        User invitedUser = userRepository.findByEmail("casey.caregiver@example.com").orElseThrow();
        assertThat(invitedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(invitedUser.getPhone()).isEqualTo("555-000-9999");
    }

    @Test
    void invalidInvitationTokenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/invitations/{token}", "missing-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invitingSameEmailTwiceReplacesPendingInvite() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(post("/api/users/invitations")
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "email": "casey.duplicate@example.com",
                                  "phone": null,
                                  "role": "CAREGIVER",
                                  "branchIds": []
                                }
                                """))
                .andExpect(status().isOk());

        UUID firstInvitationId = userInvitationRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/users/invitations")
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "email": "casey.duplicate@example.com",
                                  "phone": null,
                                  "role": "CAREGIVER",
                                  "branchIds": []
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(userInvitationRepository.findAll())
                .hasSize(2)
                .anySatisfy(invitation -> {
                    if (invitation.getId().equals(firstInvitationId)) {
                        assertThat(invitation.getStatus()).isEqualTo(UserInvitationStatus.CANCELLED);
                    }
                })
                .anySatisfy(invitation -> assertThat(invitation.getStatus()).isEqualTo(UserInvitationStatus.PENDING));
    }

    @Test
    void unauthorizedMembershipCannotSendInvite() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership caregiverMembership = createMembership(createUser("Casey", "Caregiver"), agency, AgencyRole.CAREGIVER);

        mockMvc.perform(post("/api/users/invitations")
                        .with(authentication(authenticationFor(caregiverMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "email": "casey.caregiver@example.com",
                                  "phone": null,
                                  "role": "CAREGIVER",
                                  "branchIds": []
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBranchReturnsBadRequest() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);

        mockMvc.perform(post("/api/users/invitations")
                        .with(authentication(authenticationFor(actorMembership)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Caregiver",
                                  "email": "casey.invalid-branch@example.com",
                                  "phone": null,
                                  "role": "CAREGIVER",
                                  "branchIds": ["%s"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredInvitationReturnsGone() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        User invitedUser = createUser("Casey", "Invited");
        AgencyMembership invitedMembership = createMembership(invitedUser, agency, AgencyRole.CAREGIVER);

        UserInvitation invitation = userInvitationRepository.saveAndFlush(UserInvitation.issue(
                actorMembership,
                invitedMembership,
                invitedUser,
                invitedUser.getEmail(),
                "expired-token",
                java.time.OffsetDateTime.now().minusMinutes(5)));

        mockMvc.perform(get("/api/invitations/{token}", invitation.getToken()))
                .andExpect(status().isGone());
    }

    @Test
    void alreadyUsedInvitationReturnsConflict() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        AgencyMembership actorMembership = createMembership(createUser("Alicia", "Owner"), agency, AgencyRole.AGENCY_OWNER);
        User invitedUser = createUser("Casey", "Invited");
        AgencyMembership invitedMembership = createMembership(invitedUser, agency, AgencyRole.CAREGIVER);
        UserInvitation invitation = userInvitationRepository.saveAndFlush(UserInvitation.issue(
                actorMembership,
                invitedMembership,
                invitedUser,
                invitedUser.getEmail(),
                "used-token",
                java.time.OffsetDateTime.now().plusDays(1)));
        invitation.accept();
        userInvitationRepository.saveAndFlush(invitation);

        mockMvc.perform(post("/api/invitations/{token}/accept", invitation.getToken())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Casey",
                                  "lastName": "Invited",
                                  "phone": null,
                                  "password": "ValidPassword!123"
                                }
                                """))
                .andExpect(status().isConflict());
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
