package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.auth.application.TotpService;
import com.homehealthcare.auth.domain.MfaEnrollmentChallenge;
import com.homehealthcare.auth.domain.MfaEnrollmentChallengeRepository;
import com.homehealthcare.auth.domain.MfaEnrollmentChallengeStatus;
import com.homehealthcare.auth.domain.UserMfaRecoveryCodeRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.time.Instant;
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
class MfaEnrollmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TotpService totpService;

    @Autowired
    private MfaEnrollmentChallengeRepository mfaEnrollmentChallengeRepository;

    @Autowired
    private UserMfaRecoveryCodeRepository userMfaRecoveryCodeRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void totpEnrollmentGeneratesRecoveryCodesRequiresPasswordReauthAndExposesStatus() throws Exception {
        User user = activeUser("alicia.mfa@example.com");
        SessionFixture session = login(user.getEmail(), "StartPassword1!");

        mockMvc.perform(get("/api/auth/mfa/status")
                        .header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(false))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(0));

        MvcResult startResult = mockMvc.perform(post("/api/auth/mfa/enrollment/start")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + session.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualEntryKey").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andExpect(jsonPath("$.recoveryCodes.length()").value(8))
                .andReturn();

        JsonNode payload = new ObjectMapper().readTree(startResult.getResponse().getContentAsByteArray());
        String enrollmentToken = payload.get("enrollmentToken").asText();
        String manualEntryKey = payload.get("manualEntryKey").asText();
        String currentCode = totpService.generateCurrentCode(manualEntryKey, Instant.now());

        mockMvc.perform(post("/api/auth/mfa/enrollment/confirm")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentToken": "%s",
                                  "totpCode": "%s"
                                }
                                """.formatted(enrollmentToken, currentCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(8));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.isMfaEnabled()).isTrue();
        assertThat(savedUser.getMfaSecret()).isEqualTo(manualEntryKey);
        assertThat(savedUser.getMfaEnrolledAt()).isNotNull();
        assertThat(userMfaRecoveryCodeRepository.countByUser_IdAndConsumedAtIsNull(user.getId())).isEqualTo(8);

        MfaEnrollmentChallenge challenge = mfaEnrollmentChallengeRepository.findByToken(enrollmentToken).orElseThrow();
        assertThat(challenge.getStatus()).isEqualTo(MfaEnrollmentChallengeStatus.COMPLETED);

        mockMvc.perform(get("/api/auth/mfa/status")
                        .cookie(new org.springframework.mock.web.MockCookie(AuthCookieSupport.ACCESS_TOKEN_COOKIE, session.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaEnabled").value(true))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(8))
                .andExpect(jsonPath("$.enrolledAt").exists());

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events).extracting(AuditEvent::getActionType)
                .contains("MFA_ENROLLMENT_STARTED", "MFA_ENROLLED");
    }

    @Test
    void enrollmentRejectsBadPasswordInvalidTotpAndExpiredChallenge() throws Exception {
        User user = activeUser("casey.mfa@example.com");
        SessionFixture session = login(user.getEmail(), "StartPassword1!");

        mockMvc.perform(post("/api/auth/mfa/enrollment/start")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + session.accessToken())
                        .content("""
                                {
                                  "currentPassword": "WrongPassword1!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Current password is incorrect"
                        }
                        """));

        MvcResult startResult = mockMvc.perform(post("/api/auth/mfa/enrollment/start")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + session.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = new ObjectMapper().readTree(startResult.getResponse().getContentAsByteArray());
        String enrollmentToken = payload.get("enrollmentToken").asText();

        mockMvc.perform(post("/api/auth/mfa/enrollment/confirm")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentToken": "%s",
                                  "totpCode": "000000"
                                }
                                """.formatted(enrollmentToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "TOTP code is invalid"
                        }
                        """));

        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId()))
                .filteredOn(event -> event.getActionType().equals("MFA_ENROLLMENT_FAILED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"reason\":\"INVALID_TOTP_CODE\""));

        MfaEnrollmentChallenge challenge = mfaEnrollmentChallengeRepository.findByToken(enrollmentToken).orElseThrow();
        challenge.rescheduleExpiration(OffsetDateTime.now().minusMinutes(1));
        challenge = mfaEnrollmentChallengeRepository.saveAndFlush(challenge);
        challenge.getId();

        mockMvc.perform(post("/api/auth/mfa/enrollment/confirm")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "enrollmentToken": "%s",
                                  "totpCode": "000000"
                                }
                                """.formatted(enrollmentToken)))
                .andExpect(status().isGone())
                .andExpect(content().json("""
                        {
                          "message": "MFA enrollment token has expired"
                        }
                        """));
    }

    private User activeUser(String email) {
        User user = userRepository.saveAndFlush(User.invite("Test", "User", email, null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        return userRepository.saveAndFlush(user);
    }

    private SessionFixture login(String email, String password) throws Exception {
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
        return new SessionFixture(payload.get("accessToken").asText());
    }

    record SessionFixture(String accessToken) {
    }
}
