package com.homehealthcare.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserSessionRevocationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private UserStatusManagementService userStatusManagementService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void adminDeactivationRevokesUserSessionsAndAuditsEachRevocation() throws Exception {
        Agency agency = agencyRepository.saveAndFlush(
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"));
        User admin = activeUser("alicia.deactivate@example.com");
        AgencyMembership adminMembership = agencyMembershipRepository.saveAndFlush(
                AgencyMembership.grant(admin, agency, AgencyRole.AGENCY_OWNER));

        User target = activeUser("casey.deactivate@example.com");
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(target, agency, AgencyRole.CAREGIVER));

        UUID firstSessionId = loginAndReturnSessionId(target.getEmail(), "StartPassword1!");
        UUID secondSessionId = loginAndReturnSessionId(target.getEmail(), "StartPassword1!");

        userStatusManagementService.changeStatus(adminMembership, target.getId(), UserStatus.DEACTIVATED);

        AuthSession firstSession = authSessionRepository.findById(firstSessionId).orElseThrow();
        AuthSession secondSession = authSessionRepository.findById(secondSessionId).orElseThrow();
        assertThat(firstSession.isRevoked()).isTrue();
        assertThat(secondSession.isRevoked()).isTrue();
        assertThat(firstSession.getRevocationReason()).isEqualTo("USER_DEACTIVATED");
        assertThat(secondSession.getRevocationReason()).isEqualTo("USER_DEACTIVATED");

        List<AuditEvent> sessionRevocations = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(adminMembership.getId())
                .stream()
                .filter(event -> event.getActionType().equals("USER_SESSION_REVOKED"))
                .toList();
        assertThat(sessionRevocations).hasSize(2);
        assertThat(sessionRevocations).extracting(AuditEvent::getTargetId).containsExactlyInAnyOrder(firstSessionId, secondSessionId);
    }

    private User activeUser(String email) {
        User user = userRepository.saveAndFlush(User.invite("Test", "User", email, null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        return userRepository.saveAndFlush(user);
    }

    private UUID loginAndReturnSessionId(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
        return UUID.fromString(payload.get("sessionId").asText());
    }
}
