package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentAuthSessionResolver {

    private final AuthSessionRepository authSessionRepository;
    private final TokenHashingService tokenHashingService;
    private final SessionTimeoutService sessionTimeoutService;
    private final SessionSecurityProperties sessionSecurityProperties;

    public Optional<AuthSession> resolveActive(String accessToken, String sessionId) {
        if (accessToken == null || accessToken.isBlank()) {
            return Optional.empty();
        }

        return authSessionRepository.findByAccessTokenHashAndRevokedAtIsNull(
                        tokenHashingService.hash(accessToken.trim()))
                .filter(session -> matchesSessionId(session, sessionId));
    }

    public AuthSession requireActive(String accessToken, String sessionId) {
        AuthSession session = requireValidatedActiveSession(accessToken, sessionId);
        sessionTimeoutService.recordActivity(session);
        return session;
    }

    public Optional<AuthSession> resolveRefresh(String refreshToken, String sessionId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        return authSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(
                        tokenHashingService.hash(refreshToken.trim()))
                .filter(session -> matchesSessionId(session, sessionId));
    }

    public AuthSession requireRefresh(String refreshToken, String sessionId) {
        AuthSession session = resolveRefresh(refreshToken, sessionId)
                .orElseThrow(CurrentAuthSessionNotFoundException::new);
        if (session.isRefreshTokenExpiredAt(OffsetDateTime.now()) || !sessionTimeoutService.enforceTimeouts(session)) {
            throw new CurrentAuthSessionNotFoundException();
        }
        return session;
    }

    public Optional<AuthSession> resolve(String accessToken, String refreshToken, String sessionId) {
        if (accessToken != null && !accessToken.isBlank()) {
            return authSessionRepository.findByAccessTokenHashAndRevokedAtIsNull(
                            tokenHashingService.hash(accessToken.trim()))
                    .filter(session -> matchesSessionId(session, sessionId));
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            return authSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(
                            tokenHashingService.hash(refreshToken.trim()))
                    .filter(session -> matchesSessionId(session, sessionId));
        }

        if (sessionId != null && !sessionId.isBlank()) {
            try {
                return authSessionRepository.findByIdAndRevokedAtIsNull(UUID.fromString(sessionId.trim()));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public AuthSession require(String accessToken, String refreshToken, String sessionId) {
        return resolve(accessToken, refreshToken, sessionId)
                .orElseThrow(CurrentAuthSessionNotFoundException::new);
    }

    private boolean matchesSessionId(AuthSession session, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return true;
        }
        try {
            return session.getId().equals(UUID.fromString(sessionId.trim()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public SessionSnapshot requireActiveSnapshot(String accessToken, String sessionId) {
        AuthSession session = requireValidatedActiveSession(accessToken, sessionId);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime idleExpiresAt = session.idleTimeoutAt(sessionSecurityProperties.getIdleTimeout());
        OffsetDateTime forcedLogoutAt = session.forcedLogoutAt(sessionSecurityProperties.getIdleTimeout());
        OffsetDateTime warningStartsAt = forcedLogoutAt.minus(sessionSecurityProperties.getWarningWindow());
        return new SessionSnapshot(
                session.getId(),
                session.getUser().getId(),
                idleExpiresAt,
                session.getAbsoluteExpiresAt(),
                forcedLogoutAt,
                !warningStartsAt.isAfter(now),
                Math.max(0, java.time.Duration.between(now, forcedLogoutAt).getSeconds()));
    }

    public record SessionSnapshot(
            UUID sessionId,
            UUID userId,
            OffsetDateTime idleTimeoutAt,
            OffsetDateTime absoluteTimeoutAt,
            OffsetDateTime forcedLogoutAt,
            boolean warningRequired,
            long secondsUntilForcedLogout) {
    }

    private AuthSession requireValidatedActiveSession(String accessToken, String sessionId) {
        AuthSession session = resolveActive(accessToken, sessionId)
                .orElseThrow(CurrentAuthSessionNotFoundException::new);
        if (session.isAccessTokenExpiredAt(OffsetDateTime.now()) || !sessionTimeoutService.enforceTimeouts(session)) {
            throw new CurrentAuthSessionNotFoundException();
        }
        return session;
    }
}
