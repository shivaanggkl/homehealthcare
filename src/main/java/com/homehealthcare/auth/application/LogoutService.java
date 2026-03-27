package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class LogoutService {

    private static final String DEFAULT_REDIRECT_TO = "/login?logout=success";
    private static final String ACTION_USER_LOGGED_OUT = "USER_LOGGED_OUT";

    private final AuthSessionRepository authSessionRepository;
    private final TokenHashingService tokenHashingService;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public LogoutResult logout(LogoutCommand command) {
        Optional<AuthSession> session = resolveActiveSession(command);
        session.ifPresent(authSession -> {
            authSession.revoke("USER_LOGOUT", OffsetDateTime.now());
            auditEventRepository.save(AuditEvent.create(
                    "USER",
                    authSession.getUser().getId(),
                    authSession.getUser().getEmail(),
                    ACTION_USER_LOGGED_OUT,
                    "AUTH_SESSION",
                    authSession.getId(),
                    null,
                    "{\"sessionId\":\"" + authSession.getId() + "\"}"));
        });

        return new LogoutResult(resolveRedirectTo(command.redirectTo()), session.map(AuthSession::getId).orElse(null));
    }

    private Optional<AuthSession> resolveActiveSession(LogoutCommand command) {
        if (command.accessToken() != null && !command.accessToken().isBlank()) {
            return authSessionRepository.findByAccessTokenHashAndRevokedAtIsNull(
                    tokenHashingService.hash(command.accessToken().trim()));
        }

        if (command.refreshToken() != null && !command.refreshToken().isBlank()) {
            return authSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(
                    tokenHashingService.hash(command.refreshToken().trim()));
        }

        if (command.sessionId() != null && !command.sessionId().isBlank()) {
            try {
                return authSessionRepository.findByIdAndRevokedAtIsNull(UUID.fromString(command.sessionId().trim()));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static String resolveRedirectTo(String redirectTo) {
        if (redirectTo == null || redirectTo.isBlank()) {
            return DEFAULT_REDIRECT_TO;
        }
        String normalized = redirectTo.trim();
        if (normalized.startsWith("/") && !normalized.startsWith("//")) {
            return normalized;
        }
        return DEFAULT_REDIRECT_TO;
    }

    public record LogoutCommand(
            String accessToken,
            String refreshToken,
            String sessionId,
            @Pattern(regexp = "^(/(?!/).*)?$", message = "redirectTo must be a relative path") String redirectTo) {
    }

    public record LogoutResult(
            String redirectTo,
            UUID revokedSessionId) {
    }
}
