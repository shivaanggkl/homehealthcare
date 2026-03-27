package com.homehealthcare.auth.application;

import com.homehealthcare.user.domain.User;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface SessionTokenService {

    IssuedSession issueFor(@NotNull User user);

    record IssuedSession(
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {
    }
}
