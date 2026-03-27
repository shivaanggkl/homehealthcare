package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.AuthSessionRepository;
import com.homehealthcare.user.domain.User;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OpaqueSessionTokenService implements SessionTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthSessionRepository authSessionRepository;
    private final TokenHashingService tokenHashingService;

    @Override
    @Transactional
    public IssuedSession issueFor(User user) {
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime accessTokenExpiresAt = issuedAt.plusMinutes(15);
        OffsetDateTime refreshTokenExpiresAt = issuedAt.plusDays(30);
        String accessToken = generateOpaqueToken();
        String refreshToken = generateOpaqueToken();

        AuthSession authSession = authSessionRepository.save(AuthSession.issue(
                user,
                tokenHashingService.hash(accessToken),
                accessTokenExpiresAt,
                tokenHashingService.hash(refreshToken),
                refreshTokenExpiresAt));

        return new IssuedSession(
                authSession.getId(),
                accessToken,
                accessTokenExpiresAt,
                refreshToken,
                refreshTokenExpiresAt);
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
