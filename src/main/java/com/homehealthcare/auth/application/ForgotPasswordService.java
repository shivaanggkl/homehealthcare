package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.PasswordResetToken;
import com.homehealthcare.auth.domain.PasswordResetTokenRepository;
import com.homehealthcare.auth.domain.PasswordResetTokenStatus;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.platform.email.SystemEmailTemplateService;
import com.homehealthcare.platform.email.TemplatedEmail;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ForgotPasswordService {

    private static final String GENERIC_MESSAGE = "If an account exists for that email, a password reset email will be sent.";
    private static final String ACTION_PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    private static final Duration DEFAULT_RESET_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetEmailSender passwordResetEmailSender;
    private final AuditEventRepository auditEventRepository;
    private final SystemEmailTemplateService systemEmailTemplateService;

    @Transactional
    public ForgotPasswordResult requestReset(@Valid ForgotPasswordCommand command) {
        userRepository.findByEmail(normalizeEmail(command.email()))
                .filter(ForgotPasswordService::canRequestReset)
                .ifPresent(this::issueResetToken);

        return new ForgotPasswordResult(GENERIC_MESSAGE);
    }

    private void issueResetToken(User user) {
        passwordResetTokenRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        PasswordResetTokenStatus.PENDING)
                .ifPresent(existing -> passwordResetTokenRepository.save(cancel(existing)));

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(DEFAULT_RESET_TTL);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = passwordResetTokenRepository.save(
                PasswordResetToken.issue(user, user.getEmail(), token, expiresAt));

        TemplatedEmail templatedEmail = systemEmailTemplateService.composePasswordResetEmail(
                user.getFirstName(),
                token,
                expiresAt);

        passwordResetEmailSender.send(new PasswordResetEmailSender.PasswordResetEmail(
                resetToken.getId(),
                user.getId(),
                user.getEmail(),
                templatedEmail.subject(),
                templatedEmail.textBody(),
                templatedEmail.htmlBody(),
                templatedEmail.actionUrl(),
                expiresAt));

        auditEventRepository.save(AuditEvent.create(
                "USER",
                user.getId(),
                user.getEmail(),
                ACTION_PASSWORD_RESET_REQUESTED,
                "PASSWORD_RESET_TOKEN",
                resetToken.getId(),
                null,
                "{\"expiresAt\":\"" + expiresAt + "\"}"));
    }

    private static PasswordResetToken cancel(PasswordResetToken existing) {
        existing.cancel();
        return existing;
    }

    private static boolean canRequestReset(User user) {
        return user.hasPasswordHash()
                && user.getStatus() != UserStatus.DEACTIVATED
                && user.getStatus() != UserStatus.INVITED;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record ForgotPasswordCommand(
            @NotBlank @Email String email) {
    }

    public record ForgotPasswordResult(String message) {
    }
}
