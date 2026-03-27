package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentAuthSessionResolver {

    private final AuthSessionRepository authSessionRepository;
    private final TokenHashingService tokenHashingService;

    public Optional<AuthSession> resolve(String accessToken, String refreshToken, String sessionId) {
        if (accessToken != null && !accessToken.isBlank()) {
            return authSessionRepository.findByAccessTokenHashAndRevokedAtIsNull(
                    tokenHashingService.hash(accessToken.trim()));
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            return authSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(
                    tokenHashingService.hash(refreshToken.trim()));
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
}
