package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RefreshSessionIntegrationTest {

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
    void refreshRotatesSessionAndIssuesNewSecureCookies() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.refresh@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture session = login(user.getEmail(), "StartPassword1!");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(
                                new MockCookie(AuthCookieSupport.REFRESH_TOKEN_COOKIE, session.refreshToken()),
                                new MockCookie(AuthCookieSupport.SESSION_ID_COOKIE, session.sessionId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode payload = new ObjectMapper().readTree(refreshResult.getResponse().getContentAsByteArray());
        UUID newSessionId = UUID.fromString(payload.get("sessionId").asText());
        assertThat(newSessionId).isNotEqualTo(session.sessionId());

        AuthSession previousSession = authSessionRepository.findById(session.sessionId()).orElseThrow();
        AuthSession refreshedSession = authSessionRepository.findById(newSessionId).orElseThrow();
        assertThat(previousSession.isRevoked()).isTrue();
        assertThat(previousSession.getRevocationReason()).isEqualTo("TOKEN_REFRESHED");
        assertThat(refreshedSession.isRevoked()).isFalse();

        assertThat(refreshResult.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(value -> assertThat(value).contains("hhc_access_token=").contains("HttpOnly").contains("Secure"))
                .anySatisfy(value -> assertThat(value).contains("hhc_refresh_token=").contains("HttpOnly").contains("Secure"))
                .anySatisfy(value -> assertThat(value).contains("hhc_session_id=").contains("HttpOnly").contains("Secure"));

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("USER_SESSION_REFRESHED"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains(session.sessionId().toString()));
    }

    @Test
    void refreshRejectsExpiredRefreshToken() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Casey",
                "Caregiver",
                "casey.refresh@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture session = login(user.getEmail(), "StartPassword1!");
        AuthSession authSession = authSessionRepository.findById(session.sessionId()).orElseThrow();
        java.lang.reflect.Field field = AuthSession.class.getDeclaredField("refreshTokenExpiresAt");
        field.setAccessible(true);
        field.set(authSession, java.time.OffsetDateTime.now().minusMinutes(1));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(
                                new MockCookie(AuthCookieSupport.REFRESH_TOKEN_COOKIE, session.refreshToken()),
                                new MockCookie(AuthCookieSupport.SESSION_ID_COOKIE, session.sessionId().toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRejectsAbsoluteTimedOutSessionAndAuditsTimeout() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Taylor",
                "Scheduler",
                "taylor.refresh@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture session = login(user.getEmail(), "StartPassword1!");
        AuthSession authSession = authSessionRepository.findById(session.sessionId()).orElseThrow();
        setField(authSession, "absoluteExpiresAt", java.time.OffsetDateTime.now().minusMinutes(1));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(
                                new MockCookie(AuthCookieSupport.REFRESH_TOKEN_COOKIE, session.refreshToken()),
                                new MockCookie(AuthCookieSupport.SESSION_ID_COOKIE, session.sessionId().toString())))
                .andExpect(status().isUnauthorized());

        AuthSession savedSession = authSessionRepository.findById(session.sessionId()).orElseThrow();
        assertThat(savedSession.isRevoked()).isTrue();
        assertThat(savedSession.getRevocationReason()).isEqualTo("SESSION_ABSOLUTE_TIMEOUT");
        assertThat(auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId()))
                .filteredOn(event -> event.getActionType().equals("USER_SESSION_TIMED_OUT"))
                .singleElement()
                .satisfies(event -> assertThat(event.getMetadataJson()).contains("\"timeoutType\":\"absolute\""));
    }

    private static void setField(AuthSession authSession, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = AuthSession.class.getDeclaredField(fieldName);
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
                payload.get("accessToken").asText(),
                payload.get("refreshToken").asText());
    }

    record SessionFixture(UUID sessionId, String accessToken, String refreshToken) {
    }
}
