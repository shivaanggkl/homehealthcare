package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class RefreshSessionService {

    private static final String ACTION_USER_SESSION_REFRESHED = "USER_SESSION_REFRESHED";

    private final CurrentAuthSessionResolver currentAuthSessionResolver;
    private final SessionTokenService sessionTokenService;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public RefreshedSession refresh(RefreshCommand command) {
        AuthSession currentSession = currentAuthSessionResolver.requireRefresh(
                command.refreshToken(),
                command.sessionId());
        User user = currentSession.getUser();

        currentSession.revoke("TOKEN_REFRESHED", OffsetDateTime.now());
        SessionTokenService.IssuedSession issuedSession = sessionTokenService.issueFor(user);

        auditEventRepository.save(AuditEvent.create(
                "USER",
                user.getId(),
                user.getEmail(),
                ACTION_USER_SESSION_REFRESHED,
                "AUTH_SESSION",
                issuedSession.sessionId(),
                null,
                "{\"previousSessionId\":\"" + currentSession.getId()
                        + "\",\"newSessionId\":\"" + issuedSession.sessionId() + "\"}"));

        return new RefreshedSession(
                user.getId(),
                issuedSession.sessionId(),
                issuedSession.accessToken(),
                issuedSession.accessTokenExpiresAt(),
                issuedSession.refreshToken(),
                issuedSession.refreshTokenExpiresAt());
    }

    public record RefreshCommand(
            @NotBlank String refreshToken,
            @NotBlank String sessionId) {
    }

    public record RefreshedSession(
            UUID userId,
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {
    }
}
