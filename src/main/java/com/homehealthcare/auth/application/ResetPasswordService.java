package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.PasswordResetToken;
import com.homehealthcare.auth.domain.PasswordResetTokenRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.application.UserSessionService;
import com.homehealthcare.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.OffsetDateTime;

@Service
@Validated
@RequiredArgsConstructor
public class ResetPasswordService {

    private static final String ACTION_PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final UserSessionService userSessionService;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public ResetPasswordResult resetPassword(@Valid ResetPasswordCommand command) {
        PasswordResetToken resetToken = loadPendingResetToken(command.token());
        passwordPolicy.validate(command.newPassword());

        User user = resetToken.getUser();
        user.updatePassword(passwordEncoder.encode(command.newPassword()));
        resetToken.consume();

        if (command.revokeExistingSessions()) {
            userSessionService.revokeAllSessions(user.getId(), "PASSWORD_RESET");
        }

        auditEventRepository.save(AuditEvent.create(
                "USER",
                user.getId(),
                user.getEmail(),
                ACTION_PASSWORD_RESET_COMPLETED,
                "PASSWORD_RESET_TOKEN",
                resetToken.getId(),
                null,
                "{\"revokeExistingSessions\":" + command.revokeExistingSessions() + "}"));

        return new ResetPasswordResult(
                resetToken.getId(),
                user.getId(),
                command.revokeExistingSessions());
    }

    private PasswordResetToken loadPendingResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token.trim())
                .orElseThrow(PasswordResetTokenNotFoundException::new);

        if (resetToken.isExpiredAt(OffsetDateTime.now())) {
            resetToken.expire();
            passwordResetTokenRepository.save(resetToken);
            throw new PasswordResetTokenExpiredException();
        }

        if (!resetToken.isPending()) {
            throw new PasswordResetTokenAlreadyUsedException();
        }

        return resetToken;
    }

    public record ResetPasswordCommand(
            @NotBlank String token,
            @NotBlank String newPassword,
            boolean revokeExistingSessions) {
    }

    public record ResetPasswordResult(
            java.util.UUID passwordResetTokenId,
            java.util.UUID userId,
            boolean revokedExistingSessions) {
    }
}
