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

    @Transactional
    public LoginResult login(@Valid LoginCommand command) {
        User user = userRepository.findByEmail(normalizeEmail(command.email()))
                .orElseThrow(InvalidLoginCredentialsException::new);

        if (!user.hasPasswordHash() || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidLoginCredentialsException();
        }

        try {
            userAuthenticationPolicy.requireCanSignIn(user);
        } catch (UserSignInBlockedException exception) {
            throw new InvalidLoginCredentialsException();
        }

        user.recordLogin(OffsetDateTime.now());

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

        return new LoginResult(
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

    public record LoginCommand(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record LoginResult(
            java.util.UUID userId,
            java.util.UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt) {
    }
}
