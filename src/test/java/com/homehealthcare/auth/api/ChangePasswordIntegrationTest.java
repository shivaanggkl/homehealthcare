package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
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
class ChangePasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void currentPasswordIsRequiredAndOtherSessionsCanBeInvalidated() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.change@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture currentSession = login(user.getEmail(), "StartPassword1!");
        SessionFixture otherSession = login(user.getEmail(), "StartPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "message": "Password changed successfully",
                          "invalidatedOtherSessions": true
                        }
                        """));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("ChangedPassword1!", savedUser.getPasswordHash())).isTrue();

        AuthSession persistedCurrentSession = authSessionRepository.findById(currentSession.sessionId()).orElseThrow();
        AuthSession persistedOtherSession = authSessionRepository.findById(otherSession.sessionId()).orElseThrow();
        assertThat(persistedCurrentSession.isRevoked()).isFalse();
        assertThat(persistedOtherSession.isRevoked()).isTrue();
        assertThat(persistedOtherSession.getRevocationReason()).isEqualTo("PASSWORD_CHANGED");

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("PASSWORD_CHANGED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"invalidatedOtherSessions\":true"));
    }

    @Test
    void newPasswordPolicyIsEnforcedAndCurrentPasswordMustMatch() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Casey",
                "Caregiver",
                "casey.change@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture currentSession = login(user.getEmail(), "StartPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "WrongPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Current password is incorrect"
                        }
                        """));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "weakpass",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Password must be at least 12 characters and include upper, lower, digit, and symbol characters"
                        }
                        """));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "Password123!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Password is too common or compromised. Choose a less predictable password"
                        }
                        """));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "StartPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "message": "Password cannot match any of your last 5 passwords"
                        }
                        """));
    }

    @Test
    void authenticatedSessionIsRequiredAndCurrentSessionCanBePreserved() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Taylor",
                "Scheduler",
                "taylor.change@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture currentSession = login(user.getEmail(), "StartPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {
                          "message": "Authenticated session is required"
                        }
                        """));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .cookie(
                                new org.springframework.mock.web.MockCookie(AuthCookieSupport.ACCESS_TOKEN_COOKIE, currentSession.accessToken()),
                                new org.springframework.mock.web.MockCookie(AuthCookieSupport.SESSION_ID_COOKIE, currentSession.sessionId().toString()))
                        .with(csrf().asHeader())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isOk());

        AuthSession persistedCurrentSession = authSessionRepository.findById(currentSession.sessionId()).orElseThrow();
        assertThat(persistedCurrentSession.isRevoked()).isFalse();
    }

    @Test
    void expiredAccessTokenIsRejected() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Morgan",
                "Reviewer",
                "morgan.change@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture currentSession = login(user.getEmail(), "StartPassword1!");
        AuthSession authSession = authSessionRepository.findById(currentSession.sessionId()).orElseThrow();
        Field field = AuthSession.class.getDeclaredField("accessTokenExpiresAt");
        field.setAccessible(true);
        field.set(authSession, OffsetDateTime.now().minusMinutes(1));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("""
                        {
                          "message": "Authenticated session is required"
                        }
                        """));
    }

    @Test
    void idleTimedOutSessionIsRejectedAndAudited() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Reese",
                "Coordinator",
                "reese.change@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture currentSession = login(user.getEmail(), "StartPassword1!");
        AuthSession authSession = authSessionRepository.findById(currentSession.sessionId()).orElseThrow();
        setField(authSession, "lastActivityAt", OffsetDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + currentSession.accessToken())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isUnauthorized());

        AuthSession savedSession = authSessionRepository.findById(currentSession.sessionId()).orElseThrow();
        assertThat(savedSession.isRevoked()).isTrue();
        assertThat(savedSession.getRevocationReason()).isEqualTo("SESSION_IDLE_TIMEOUT");
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId()))
                .filteredOn(event -> event.getActionType().equals("USER_SESSION_TIMED_OUT"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"timeoutType\":\"idle\""));
    }

    private static void setField(AuthSession authSession, String fieldName, Object value) throws Exception {
        Field field = AuthSession.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(authSession, value);
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
        return new SessionFixture(
                UUID.fromString(payload.get("sessionId").asText()),
                payload.get("accessToken").asText());
    }

    record SessionFixture(UUID sessionId, String accessToken) {
    }
}
