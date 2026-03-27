package com.homehealthcare.auth.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Test
    void sessionStatusExposesWarningWindowBeforeForcedLogout() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.session@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        userRepository.saveAndFlush(user);

        SessionFixture session = login(user.getEmail(), "StartPassword1!");
        AuthSession authSession = authSessionRepository.findById(session.sessionId()).orElseThrow();
        setField(authSession, "lastActivityAt", OffsetDateTime.now().minusMinutes(29));
        setField(authSession, "absoluteExpiresAt", OffsetDateTime.now().plusHours(4));

        mockMvc.perform(get("/api/auth/session")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .header("X-Session-Id", session.sessionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.sessionId().toString()))
                .andExpect(jsonPath("$.warningRequired").value(true))
                .andExpect(jsonPath("$.forcedLogoutAt").exists())
                .andExpect(jsonPath("$.secondsUntilForcedLogout").isNumber());
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

    private static void setField(AuthSession authSession, String fieldName, Object value) throws Exception {
        Field field = AuthSession.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(authSession, value);
    }

    record SessionFixture(UUID sessionId, String accessToken) {
    }
}
