package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.agency.domain.AgencyRepository;
import com.homehealthcare.auth.application.TotpService;
import com.homehealthcare.auth.domain.UserMfaRecoveryCode;
import com.homehealthcare.auth.domain.UserMfaRecoveryCodeRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
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
class MfaLoginChallengeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private AgencyMembershipRepository agencyMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotpService totpService;

    @Autowired
    private UserMfaRecoveryCodeRepository userMfaRecoveryCodeRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void mfaChallengeAppearsAfterCorrectPasswordAndTotpValidationWorks() throws Exception {
        User user = enrolledMfaUser("alicia.mfalogin@example.com");
        Agency agency = agencyRepository.saveAndFlush(Agency.create("North Star", "north-star-" + user.getId(), "America/Chicago", "ops@northstar.example"));
        agency.requireMfaForAllUsers();
        agencyRepository.saveAndFlush(agency);
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.CAREGIVER));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StartPassword1!"
                                }
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.loginChallengeToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        JsonNode payload = new ObjectMapper().readTree(loginResult.getResponse().getContentAsByteArray());
        String challengeToken = payload.get("loginChallengeToken").asText();
        String totpCode = totpService.generateCurrentCode(user.getMfaSecret(), java.time.Instant.now());

        mockMvc.perform(post("/api/auth/login/mfa")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeToken": "%s",
                                  "totpCode": "%s"
                                }
                                """.formatted(challengeToken, totpCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.recoveryCodeUsed").value(false));

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("MFA_LOGIN_CHALLENGE_ISSUED"))
                .singleElement();
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("MFA_LOGIN_CHALLENGE_COMPLETED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"recoveryCodeUsed\":false"));
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("USER_LOGGED_IN"))
                .anySatisfy(event -> assertThat(event.getMetadataJson()).contains("EMAIL_PASSWORD_MFA"));
    }

    @Test
    void recoveryCodeFallbackWorksAndAgencyPolicyCanTargetRoles() throws Exception {
        User caregiver = enrolledMfaUser("casey.caregiver@example.com");
        User auditor = enrolledMfaUser("alex.auditor@example.com");
        Agency agency = agencyRepository.saveAndFlush(Agency.create("Sunrise", "sunrise-" + caregiver.getId(), "America/Chicago", "ops@sunrise.example"));
        agency.requireMfaForRoles(java.util.Set.of(AgencyRole.CAREGIVER));
        agencyRepository.saveAndFlush(agency);
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(caregiver, agency, AgencyRole.CAREGIVER));
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(auditor, agency, AgencyRole.READ_ONLY_AUDITOR));

        MvcResult caregiverLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StartPassword1!"
                                }
                                """.formatted(caregiver.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn();

        JsonNode caregiverPayload = new ObjectMapper().readTree(caregiverLogin.getResponse().getContentAsByteArray());
        String challengeToken = caregiverPayload.get("loginChallengeToken").asText();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StartPassword1!"
                                }
                                """.formatted(auditor.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(false))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        String recoveryCode = "RCVR-0001";
        userMfaRecoveryCodeRepository.deleteAllByUser_Id(caregiver.getId());
        userMfaRecoveryCodeRepository.saveAndFlush(UserMfaRecoveryCode.issue(caregiver, sha256(recoveryCode), 0));

        mockMvc.perform(post("/api/auth/login/mfa")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeToken": "%s",
                                  "recoveryCode": "%s"
                                }
                                """.formatted(challengeToken, recoveryCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodeUsed").value(true));

        UserMfaRecoveryCode consumedCode = userMfaRecoveryCodeRepository.findAllByUser_IdOrderByOrdinalAsc(caregiver.getId()).getFirst();
        assertThat(consumedCode.getConsumedAt()).isNotNull();
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(caregiver.getId()))
                .filteredOn(event -> event.getActionType().equals("MFA_LOGIN_CHALLENGE_COMPLETED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"recoveryCodeUsed\":true"));
    }

    @Test
    void invalidMfaChallengeAttemptIsAudited() throws Exception {
        User user = enrolledMfaUser("jamie.failedmfa@example.com");
        Agency agency = agencyRepository.saveAndFlush(Agency.create("North Ridge", "north-ridge-" + user.getId(), "America/Chicago", "ops@northridge.example"));
        agency.requireMfaForAllUsers();
        agencyRepository.saveAndFlush(agency);
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.CAREGIVER));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StartPassword1!"
                                }
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andReturn();

        JsonNode payload = new ObjectMapper().readTree(loginResult.getResponse().getContentAsByteArray());
        String challengeToken = payload.get("loginChallengeToken").asText();

        mockMvc.perform(post("/api/auth/login/mfa")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeToken": "%s",
                                  "totpCode": "000000"
                                }
                                """.formatted(challengeToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "TOTP code is invalid"
                        }
                        """));

        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId()))
                .filteredOn(event -> event.getActionType().equals("MFA_LOGIN_CHALLENGE_FAILED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"reason\":\"INVALID_TOTP_CODE\""));
    }

    @Test
    void protectedUsersWithoutEnrollmentAreBlockedByPolicy() throws Exception {
        User user = userRepository.saveAndFlush(User.invite("No", "Mfa", "nomfa@example.com", null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        Agency agency = agencyRepository.saveAndFlush(Agency.create("Policy", "policy-" + user.getId(), "America/Chicago", "ops@policy.example"));
        agency.requireMfaForAllUsers();
        agencyRepository.saveAndFlush(agency);
        agencyMembershipRepository.saveAndFlush(AgencyMembership.grant(user, agency, AgencyRole.CAREGIVER));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nomfa@example.com",
                                  "password": "StartPassword1!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().json("""
                        {
                          "message": "MFA enrollment is required before login can complete"
                        }
                        """));
    }

    private User enrolledMfaUser(String email) {
        User user = userRepository.saveAndFlush(User.invite("Mfa", "User", email, null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        user.enrollMfa(totpService.generateSecret(), OffsetDateTime.now());
        return userRepository.saveAndFlush(user);
    }

    private static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
