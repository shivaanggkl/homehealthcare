package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.auth.domain.PasswordResetToken;
import com.homehealthcare.auth.domain.PasswordResetTokenRepository;
import com.homehealthcare.auth.domain.PasswordResetTokenStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResetPasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void validResetTokenResetsPasswordOptionallyRevokesSessionsAndAudits() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.reset.complete@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        UUID sessionId = loginAndReturnSessionId(user.getEmail(), "StartPassword1!");
        PasswordResetToken resetToken = passwordResetTokenRepository.saveAndFlush(
                PasswordResetToken.issue(user, user.getEmail(), "valid-reset-token", OffsetDateTime.now().plusHours(1)));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "valid-reset-token",
                                  "newPassword": "NewSecurePass1!",
                                  "revokeExistingSessions": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "message": "Password reset successful",
                          "revokedExistingSessions": true
                        }
                        """));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewSecurePass1!", savedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("StartPassword1!", savedUser.getPasswordHash())).isFalse();

        PasswordResetToken consumedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
        assertThat(consumedToken.getStatus()).isEqualTo(PasswordResetTokenStatus.CONSUMED);

        AuthSession revokedSession = authSessionRepository.findById(sessionId).orElseThrow();
        assertThat(revokedSession.isRevoked()).isTrue();
        assertThat(revokedSession.getRevocationReason()).isEqualTo("PASSWORD_RESET");

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("PASSWORD_RESET_COMPLETED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"revokeExistingSessions\":true"));
    }

    @Test
    void passwordPolicyIsEnforcedAndTokenRemainsPendingOnWeakPassword() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Casey",
                "Caregiver",
                "casey.reset.policy@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        PasswordResetToken resetToken = passwordResetTokenRepository.saveAndFlush(
                PasswordResetToken.issue(user, user.getEmail(), "weak-password-token", OffsetDateTime.now().plusHours(1)));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "weak-password-token",
                                  "newPassword": "weakpass",
                                  "revokeExistingSessions": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Password must be at least 12 characters and include upper, lower, digit, and symbol characters"
                        }
                        """));

        PasswordResetToken pendingToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
        assertThat(pendingToken.getStatus()).isEqualTo(PasswordResetTokenStatus.PENDING);
    }

    @Test
    void invalidAndExpiredTokensAreRejected() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Jordan",
                "Nurse",
                "jordan.reset.expired@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        PasswordResetToken expiredToken = passwordResetTokenRepository.saveAndFlush(
                PasswordResetToken.issue(user, user.getEmail(), "expired-reset-token", OffsetDateTime.now().minusMinutes(1)));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "missing-token",
                                  "newPassword": "NewSecurePass1!",
                                  "revokeExistingSessions": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Password reset token is invalid"
                        }
                        """));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "expired-reset-token",
                                  "newPassword": "NewSecurePass1!",
                                  "revokeExistingSessions": false
                                }
                                """))
                .andExpect(status().isGone())
                .andExpect(content().json("""
                        {
                          "message": "Password reset token has expired"
                        }
                        """));

        PasswordResetToken savedExpiredToken = passwordResetTokenRepository.findById(expiredToken.getId()).orElseThrow();
        assertThat(savedExpiredToken.getStatus()).isEqualTo(PasswordResetTokenStatus.EXPIRED);
    }

    @Test
    void resetCanPreserveExistingSessionsWhenRequested() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Taylor",
                "Scheduler",
                "taylor.reset.keep@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        UUID sessionId = loginAndReturnSessionId(user.getEmail(), "StartPassword1!");
        passwordResetTokenRepository.saveAndFlush(
                PasswordResetToken.issue(user, user.getEmail(), "keep-session-token", OffsetDateTime.now().plusHours(1)));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "keep-session-token",
                                  "newPassword": "DifferentSecure1!",
                                  "revokeExistingSessions": false
                                }
                                """))
                .andExpect(status().isOk());

        AuthSession activeSession = authSessionRepository.findById(sessionId).orElseThrow();
        assertThat(activeSession.isRevoked()).isFalse();
    }

    private UUID loginAndReturnSessionId(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int sessionIdIndex = response.indexOf("\"sessionId\":\"");
        int sessionIdStart = sessionIdIndex + "\"sessionId\":\"".length();
        int sessionIdEnd = response.indexOf('"', sessionIdStart);
        return UUID.fromString(response.substring(sessionIdStart, sessionIdEnd));
    }
}
