package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class LogoutIntegrationTest {

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
    void logoutRevokesCurrentSessionClearsCookiesAuditsAndRedirects() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.logout@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alicia.logout@example.com",
                                  "password": "S3curePassword!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = new ObjectMapper().readTree(loginResult.getResponse().getContentAsByteArray());
        UUID sessionId = UUID.fromString(loginJson.get("sessionId").asText());

        AuthSession activeSession = authSessionRepository.findById(sessionId).orElseThrow();
        assertThat(activeSession.isRevoked()).isFalse();

        MockCookie accessCookie = responseCookie(loginResult, AuthCookieSupport.ACCESS_TOKEN_COOKIE);
        MockCookie refreshCookie = responseCookie(loginResult, AuthCookieSupport.REFRESH_TOKEN_COOKIE);
        MockCookie sessionCookie = responseCookie(loginResult, AuthCookieSupport.SESSION_ID_COOKIE);

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessCookie, refreshCookie, sessionCookie)
                        .param("redirectTo", "/signed-out"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(logoutResult.getResponse().getHeader("Location")).isEqualTo("/signed-out");
        assertThat(logoutResult.getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(value -> assertThat(value).contains(AuthCookieSupport.ACCESS_TOKEN_COOKIE + "=").contains("Max-Age=0"))
                .anySatisfy(value -> assertThat(value).contains(AuthCookieSupport.REFRESH_TOKEN_COOKIE + "=").contains("Max-Age=0"))
                .anySatisfy(value -> assertThat(value).contains(AuthCookieSupport.SESSION_ID_COOKIE + "=").contains("Max-Age=0"));

        AuthSession revokedSession = authSessionRepository.findById(sessionId).orElseThrow();
        assertThat(revokedSession.isRevoked()).isTrue();
        assertThat(revokedSession.getRevocationReason()).isEqualTo("USER_LOGOUT");

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("USER_LOGGED_OUT"))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTargetId()).isEqualTo(sessionId);
                    assertThat(event.getMetadataJson()).contains(sessionId.toString());
                });
    }

    private static MockCookie responseCookie(MvcResult result, String name) {
        jakarta.servlet.http.Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).isNotNull();
        return new MockCookie(cookie.getName(), cookie.getValue());
    }
}
