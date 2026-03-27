package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.auth.domain.AuthLoginAttemptOutcome;
import com.homehealthcare.auth.domain.AuthLoginAttemptRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
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
        "security.login-protection.failure-threshold=10",
        "security.login-protection.failure-window=15m",
        "security.login-protection.temporary-lockout-duration=15m",
        "security.login-protection.max-attempts-per-ip=2",
        "security.login-protection.max-attempts-per-email=10",
        "security.login-protection.rate-limit-window=10m"
})
class FailedLoginProtectionRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthLoginAttemptRepository authLoginAttemptRepository;

    @Test
    void rateLimitingIsAppliedAndSecurityEventsAreLogged() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Casey",
                "Caregiver",
                "casey.ratelimit@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("CorrectPassword1!"));
        userRepository.saveAndFlush(user);

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .header("X-Forwarded-For", "198.51.100.24")
                            .content("""
                                    {
                                      "email": "casey.ratelimit@example.com",
                                      "password": "WrongPassword1!"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.24")
                        .content("""
                                {
                                  "email": "casey.ratelimit@example.com",
                                  "password": "CorrectPassword1!"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().json("""
                        {
                          "message": "Too many login attempts. Try again later."
                        }
                        """));

        assertThat(authLoginAttemptRepository.findAll())
                .extracting(attempt -> attempt.getOutcome())
                .contains(AuthLoginAttemptOutcome.RATE_LIMITED);
    }
}
