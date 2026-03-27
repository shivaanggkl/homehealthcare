package com.homehealthcare.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
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

@SpringBootTest(properties = {
        "security.web.allowed-origins=https://app.homehealthcare.example"
})
@AutoConfigureMockMvc
@Transactional
class SecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void securityHeadersArePresentOnAuthenticatedResponses() throws Exception {
        SessionFixture session = login(activeUser("headers@example.com"));

        mockMvc.perform(get("/api/auth/session")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .header("X-Session-Id", session.sessionId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.mfaSecret").doesNotExist());
    }

    @Test
    void corsIsLimitedToConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/auth/session")
                        .header("Origin", "https://app.homehealthcare.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.homehealthcare.example"))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("Origin")));

        MvcResult disallowed = mockMvc.perform(options("/api/auth/session")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(disallowed.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void cookieAuthenticatedStateChangingRequestsRequireCsrfButBearerRequestsDoNot() throws Exception {
        SessionFixture session = login(activeUser("csrf@example.com"));

        MockCookie accessCookie = new MockCookie("hhc_access_token", session.accessToken());
        accessCookie.setHttpOnly(true);
        MockCookie sessionCookie = new MockCookie("hhc_session_id", session.sessionId().toString());
        sessionCookie.setHttpOnly(true);

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .cookie(accessCookie, sessionCookie)
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .cookie(accessCookie, sessionCookie)
                        .with(csrf().asHeader())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isOk());

        SessionFixture bearerSession = login(activeUser("bearer-csrf@example.com"));
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearerSession.accessToken())
                        .header("X-Session-Id", bearerSession.sessionId().toString())
                        .content("""
                                {
                                  "currentPassword": "StartPassword1!",
                                  "newPassword": "ChangedPassword1!",
                                  "invalidateOtherSessions": false
                                }
                                """))
                .andExpect(status().isOk());
    }

    private User activeUser(String email) {
        User user = userRepository.saveAndFlush(User.invite("Test", "User", email, null));
        user.activateWithCredentials(passwordEncoder.encode("StartPassword1!"));
        return userRepository.saveAndFlush(user);
    }

    private SessionFixture login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "StartPassword1!"
                                }
                                """.formatted(user.getEmail())))
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
