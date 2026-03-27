package com.homehealthcare.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homehealthcare.auth.application.PasswordResetEmailSender;
import com.homehealthcare.auth.domain.PasswordResetToken;
import com.homehealthcare.auth.domain.PasswordResetTokenRepository;
import com.homehealthcare.auth.domain.PasswordResetTokenStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(ForgotPasswordIntegrationTest.TestConfig.class)
class ForgotPasswordIntegrationTest {

    private static final String GENERIC_MESSAGE = "{\"message\":\"If an account exists for that email, a password reset email will be sent.\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RecordingPasswordResetEmailSender recordingPasswordResetEmailSender;

    @BeforeEach
    void resetEmailSink() {
        recordingPasswordResetEmailSender.clear();
    }

    @Test
    void forgotPasswordIssuesExpiringTokenSendsEmailAndAuditsWithoutRevealingAccountExistence() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Alicia",
                "Admin",
                "alicia.reset@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Alicia.Reset@Example.com"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().json(GENERIC_MESSAGE));

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findAll();
        assertThat(tokens).singleElement().satisfies(token -> {
            assertThat(token.getUser().getId()).isEqualTo(user.getId());
            assertThat(token.getEmail()).isEqualTo("alicia.reset@example.com");
            assertThat(token.getStatus()).isEqualTo(PasswordResetTokenStatus.PENDING);
            assertThat(token.getExpiresAt()).isAfter(OffsetDateTime.now().plusMinutes(55));
        });

        assertThat(recordingPasswordResetEmailSender.sentEmails()).singleElement().satisfies(email -> {
            assertThat(email.userId()).isEqualTo(user.getId());
            assertThat(email.recipientEmail()).isEqualTo("alicia.reset@example.com");
            assertThat(email.subject()).contains("Reset your HomeHealthCare password");
            assertThat(email.textBody()).contains("Reset your password");
            assertThat(email.htmlBody()).contains("Reset password");
            assertThat(email.actionUrl()).contains("/reset-password");
            assertThat(email.actionUrl()).contains("signature=");
            assertThat(email.actionUrl()).contains("expiresAt=");
        });

        List<AuditEvent> auditEvents = auditEventRepository.findAllByActorIdOrderByOccurredAtAsc(user.getId());
        assertThat(auditEvents).extracting(AuditEvent::getActionType).contains("PASSWORD_RESET_REQUESTED");
    }

    @Test
    void forgotPasswordReturnsSameResponseWhenEmailDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(content().json(GENERIC_MESSAGE));

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        assertThat(recordingPasswordResetEmailSender.sentEmails()).isEmpty();
    }

    @Test
    void duplicatePendingResetRequestCancelsPreviousTokenAndIssuesFreshOne() throws Exception {
        User user = userRepository.saveAndFlush(User.invite(
                "Casey",
                "Caregiver",
                "casey.reset@example.com",
                null));
        user.activateWithCredentials(passwordEncoder.encode("S3curePassword!"));
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "casey.reset@example.com"
                                }
                                """))
                .andExpect(status().isAccepted());

        PasswordResetToken firstToken = passwordResetTokenRepository.findAll().getFirst();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "casey.reset@example.com"
                                }
                                """))
                .andExpect(status().isAccepted());

        PasswordResetToken cancelledToken = passwordResetTokenRepository.findById(firstToken.getId()).orElseThrow();
        assertThat(cancelledToken.getStatus()).isEqualTo(PasswordResetTokenStatus.CANCELLED);
        assertThat(passwordResetTokenRepository.findAll()).hasSize(2);
        assertThat(recordingPasswordResetEmailSender.sentEmails()).hasSize(2);
    }

    static class RecordingPasswordResetEmailSender implements PasswordResetEmailSender {

        private final List<PasswordResetEmail> sentEmails = new ArrayList<>();

        @Override
        public void send(PasswordResetEmail email) {
            sentEmails.add(email);
        }

        List<PasswordResetEmail> sentEmails() {
            return sentEmails;
        }

        void clear() {
            sentEmails.clear();
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        RecordingPasswordResetEmailSender recordingPasswordResetEmailSender() {
            return new RecordingPasswordResetEmailSender();
        }
    }
}
