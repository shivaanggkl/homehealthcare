package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.auth.domain.AuthLoginAttempt;
import com.homehealthcare.auth.domain.AuthLoginAttemptOutcome;
import com.homehealthcare.auth.domain.AuthLoginAttemptRepository;
import com.homehealthcare.platform.audit.domain.AuditEventOutcome;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "security.login-protection.failure-threshold=3",
        "security.login-protection.failure-window=15m",
        "security.login-protection.temporary-lockout-duration=15m",
        "security.login-protection.max-attempts-per-ip=100",
        "security.login-protection.max-attempts-per-email=100",
        "security.login-protection.rate-limit-window=1m"
})
class FailedLoginProtectionLockoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthLoginAttemptRepository authLoginAttemptRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void failedAttemptsAreTrackedAndThresholdBasedTemporaryLockoutIsEnforced() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.bruteforce@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("CorrectPassword1!"));
        userRepository.saveAndFlush(user);

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .header("X-Forwarded-For", "203.0.113.10")
                            .content("""
                                    {
                                      "email": "alicia.bruteforce@example.com",
                                      "password": "WrongPassword1!"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("""
                            {
                              "message": "Invalid email or password"
                            }
                            """));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content("""
                                {
                                  "email": "alicia.bruteforce@example.com",
                                  "password": "CorrectPassword1!"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().json("""
                        {
                          "message": "Too many login attempts. Try again later."
                        }
                        """));

        List<AuthLoginAttempt> attempts = authLoginAttemptRepository.findAll();
        assertThat(attempts)
                .filteredOn(AuthLoginAttempt::isFailure)
                .hasSize(3);
        assertThat(attempts)
                .extracting(AuthLoginAttempt::getOutcome)
                .contains(AuthLoginAttemptOutcome.LOCKED_OUT);

        assertThat(auditEventRepository.findAll())
                .filteredOn(event -> event.getActionType().equals("USER_LOGIN_FAILED"))
                .hasSize(3)
                .allSatisfy(event -> assertThat(event.getOutcome()).isEqualTo(AuditEventOutcome.FAILURE));
    }
}
