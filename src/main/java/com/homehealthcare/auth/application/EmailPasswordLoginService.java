package com.homehealthcare.auth.application;

import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.application.UserAuthenticationPolicy;
import com.homehealthcare.user.application.UserSignInBlockedException;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class EmailPasswordLoginService {

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTION_USER_LOGGED_IN = "USER_LOGGED_IN";
    private static final String TARGET_TYPE_USER = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthenticationPolicy userAuthenticationPolicy;
    private final SessionTokenService sessionTokenService;
    private final AuditEventRepository auditEventRepository;
    private final LoginProtectionService loginProtectionService;
    private final MfaPolicyService mfaPolicyService;
    private final MfaLoginChallengeService mfaLoginChallengeService;

    @Transactional
    public LoginResult login(@Valid LoginCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        String clientIpAddress = normalizeOptional(command.clientIpAddress());

        loginProtectionService.assertLoginAllowed(normalizedEmail, clientIpAddress);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            loginProtectionService.recordFailure(normalizedEmail, clientIpAddress, null, "INVALID_CREDENTIALS");
            throw new InvalidLoginCredentialsException();
        }

        if (!user.hasPasswordHash() || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            loginProtectionService.recordFailure(normalizedEmail, clientIpAddress, user, "INVALID_CREDENTIALS");
            throw new InvalidLoginCredentialsException();
        }

        try {
            userAuthenticationPolicy.requireCanSignIn(user);
        } catch (UserSignInBlockedException exception) {
            loginProtectionService.recordFailure(normalizedEmail, clientIpAddress, user, "BLOCKED_STATUS");
            throw new InvalidLoginCredentialsException();
        }

        user.recordLogin(OffsetDateTime.now());
        loginProtectionService.recordSuccess(normalizedEmail, clientIpAddress, user);

        boolean mfaRequired = user.isMfaEnabled() && mfaPolicyService.requiresMfa(user);
        if (mfaPolicyService.requiresMfa(user) && !user.isMfaEnabled()) {
            throw new MfaEnrollmentRequiredException();
        }

        if (mfaRequired) {
            MfaLoginChallengeService.LoginChallengeResult challenge = mfaLoginChallengeService.createChallenge(user);
            return LoginResult.mfaRequired(user.getId(), challenge.challengeToken(), challenge.expiresAt());
        }

        SessionTokenService.IssuedSession issuedSession = sessionTokenService.issueFor(user);

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_USER,
                user.getId(),
                user.getEmail(),
                ACTION_USER_LOGGED_IN,
                TARGET_TYPE_USER,
                user.getId(),
                null,
                "{\"sessionId\":\"" + issuedSession.sessionId()
                        + "\",\"authenticationMethod\":\"EMAIL_PASSWORD\"}"));

        return LoginResult.authenticated(
                user.getId(),
                issuedSession.sessionId(),
                issuedSession.accessToken(),
                issuedSession.accessTokenExpiresAt(),
                issuedSession.refreshToken(),
                issuedSession.refreshTokenExpiresAt());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    public record LoginCommand(
            @NotBlank @Email String email,
            @NotBlank String password,
            String clientIpAddress) {
    }

    public record LoginResult(
            java.util.UUID userId,
            boolean mfaRequired,
            String loginChallengeToken,
            java.util.UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {

        static LoginResult authenticated(
                java.util.UUID userId,
                java.util.UUID sessionId,
                String accessToken,
                OffsetDateTime accessTokenExpiresAt,
                String refreshToken,
                OffsetDateTime refreshTokenExpiresAt) {
            return new LoginResult(userId, false, null, sessionId, accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt);
        }

        static LoginResult mfaRequired(
                java.util.UUID userId,
                String loginChallengeToken,
                OffsetDateTime challengeExpiresAt) {
            return new LoginResult(userId, true, loginChallengeToken, null, null, challengeExpiresAt, null, null);
        }
    }
}
