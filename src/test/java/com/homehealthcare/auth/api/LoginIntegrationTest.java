package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.auth.application.SessionTokenService;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(LoginIntegrationTest.TestConfig.class)
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private DeterministicSessionTokenService deterministicSessionTokenService;

    @BeforeEach
    void resetTokenIssuer() {
        deterministicSessionTokenService.clear();
    }

    @Test
    void loginWithValidEmailAndPasswordReturnsSessionTokenSetAndAudits() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Alicia@Example.com",
                                  "password": "S3curePassword!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.sessionId").value(DeterministicSessionTokenService.SESSION_ID.toString()))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.accessTokenExpiresAt").value("2026-03-27T10:15:00Z"))
                .andExpect(jsonPath("$.refreshTokenExpiresAt").value("2026-04-26T10:00:00Z"))
                .andExpect(result -> assertThat(result.getResponse().getHeaders("Set-Cookie"))
                        .anySatisfy(value -> assertThat(value)
                                .contains("hhc_access_token=")
                                .contains("HttpOnly")
                                .contains("Secure")
                                .contains("SameSite=Lax"))
                        .anySatisfy(value -> assertThat(value)
                                .contains("hhc_refresh_token=")
                                .contains("HttpOnly")
                                .contains("Secure")
                                .contains("SameSite=Lax")));

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getLastLoginAt()).isNotNull();

        List<AuditEvent> events = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(events)
                .filteredOn(event -> event.getActionType().equals("USER_LOGGED_IN"))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTargetId()).isEqualTo(user.getId());
                    assertThat(event.getActorEmail()).isEqualTo("alicia@example.com");
                    assertThat(event.getMetadataJson()).contains(DeterministicSessionTokenService.SESSION_ID.toString());
                });

        assertThat(deterministicSessionTokenService.issuedUserIds()).containsExactly(user.getId());
    }

    @Test
    void invalidEmailOrPasswordReturnsGenericUnauthorizedError() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Casey",
                "Caregiver",
                "casey@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "casey@example.com",
                                  "password": "WrongPassword!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "WrongPassword!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void lockedSuspendedAndDeactivatedUsersCannotLogIn() throws Exception {
        assertBlockedLogin(UserStatus.LOCKED, "locked@example.com");
        assertBlockedLogin(UserStatus.SUSPENDED, "suspended@example.com");
        assertBlockedLogin(UserStatus.DEACTIVATED, "deactivated@example.com");
    }

    private void assertBlockedLogin(UserStatus status, String email) throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Blocked",
                "User",
                email,
                null));
        user.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        switch (status) {
            case LOCKED -> user.lock();
            case SUSPENDED -> user.suspend();
            case DEACTIVATED -> user.deactivate();
            default -> throw new IllegalArgumentException("Unsupported blocked status: " + status);
        }
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "S3curePassword!"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    static class DeterministicSessionTokenService implements SessionTokenService {

        static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        private final java.util.List<UUID> issuedUserIds = new java.util.ArrayList<>();

        @Override
        public IssuedSession issueFor(User user) {
            issuedUserIds.add(user.getId());
            return new IssuedSession(
                    SESSION_ID,
                    "access-token",
                    OffsetDateTime.parse("2026-03-27T10:15:00Z"),
                    "refresh-token",
                    OffsetDateTime.parse("2026-04-26T10:00:00Z"));
        }

        java.util.List<UUID> issuedUserIds() {
            return issuedUserIds;
        }

        void clear() {
            issuedUserIds.clear();
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        DeterministicSessionTokenService deterministicSessionTokenService() {
            return new DeterministicSessionTokenService();
        }
    }
}
