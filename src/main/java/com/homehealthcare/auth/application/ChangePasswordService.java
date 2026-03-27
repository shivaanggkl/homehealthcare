package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ChangePasswordService {

    private static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";

    private final CurrentAuthSessionResolver currentAuthSessionResolver;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public ChangePasswordResult changePassword(@Valid ChangePasswordCommand command) {
        AuthSession currentSession = currentAuthSessionResolver.require(
                command.accessToken(),
                command.refreshToken(),
                command.sessionId());
        User user = currentSession.getUser();

        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new CurrentPasswordMismatchException();
        }

        passwordPolicy.validate(command.newPassword());
        user.updatePassword(passwordEncoder.encode(command.newPassword()));

        if (command.invalidateOtherSessions()) {
            revokeOtherSessions(user.getId(), currentSession.getId());
        }

        auditEventRepository.save(AuditEvent.create(
                "USER",
                user.getId(),
                user.getEmail(),
                ACTION_PASSWORD_CHANGED,
                "USER",
                user.getId(),
                null,
                "{\"currentSessionId\":\"" + currentSession.getId()
                        + "\",\"invalidatedOtherSessions\":" + command.invalidateOtherSessions() + "}"));

        return new ChangePasswordResult(user.getId(), currentSession.getId(), command.invalidateOtherSessions());
    }

    private void revokeOtherSessions(java.util.UUID userId, java.util.UUID currentSessionId) {
        for (AuthSession authSession : authSessionRepository.findAllByUser_IdAndRevokedAtIsNullAndIdNot(userId, currentSessionId)) {
            authSession.revoke("PASSWORD_CHANGED", OffsetDateTime.now());
        }
    }

    public record ChangePasswordCommand(
            String accessToken,
            String refreshToken,
            String sessionId,
            @NotBlank String currentPassword,
            @NotBlank String newPassword,
            boolean invalidateOtherSessions) {
    }

    public record ChangePasswordResult(
            java.util.UUID userId,
            java.util.UUID currentSessionId,
            boolean invalidatedOtherSessions) {
    }
}
