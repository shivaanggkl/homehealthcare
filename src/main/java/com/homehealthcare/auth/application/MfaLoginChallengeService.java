package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthMfaLoginChallenge;
import com.homehealthcare.auth.domain.AuthMfaLoginChallengeRepository;
import com.homehealthcare.auth.domain.AuthMfaLoginChallengeStatus;
import com.homehealthcare.auth.domain.UserMfaRecoveryCode;
import com.homehealthcare.auth.domain.UserMfaRecoveryCodeRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class MfaLoginChallengeService {

    private final AuthMfaLoginChallengeRepository authMfaLoginChallengeRepository;
    private final UserMfaRecoveryCodeRepository userMfaRecoveryCodeRepository;
    private final TotpService totpService;
    private final TokenHashingService tokenHashingService;
    private final SessionTokenService sessionTokenService;
    private final AuditEventRepository auditEventRepository;
    private final MfaProperties mfaProperties;

    @Transactional
    public LoginChallengeResult createChallenge(User user) {
        authMfaLoginChallengeRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        AuthMfaLoginChallengeStatus.PENDING)
                .ifPresent(existing -> {
                    existing.cancel();
                    authMfaLoginChallengeRepository.save(existing);
                });

        AuthMfaLoginChallenge challenge = authMfaLoginChallengeRepository.save(AuthMfaLoginChallenge.issue(
                user,
                user.getEmail(),
                UUID.randomUUID().toString(),
                OffsetDateTime.now().plus(mfaProperties.getEnrollmentChallengeTtl())));

        return new LoginChallengeResult(challenge.getToken(), challenge.getExpiresAt());
    }

    @Transactional
    public CompletedLoginResult completeChallenge(@Valid CompleteLoginChallengeCommand command) {
        AuthMfaLoginChallenge challenge = loadPendingChallenge(command.challengeToken());
        User user = challenge.getUser();

        boolean authenticatedWithRecoveryCode = false;
        if (command.totpCode() != null && !command.totpCode().isBlank()) {
            if (!totpService.verifyCode(user.getMfaSecret(), command.totpCode(), Instant.now())) {
                throw new InvalidTotpCodeException();
            }
        } else if (command.recoveryCode() != null && !command.recoveryCode().isBlank()) {
            UserMfaRecoveryCode recoveryCode = userMfaRecoveryCodeRepository.findByUser_IdAndCodeHashAndConsumedAtIsNull(
                            user.getId(),
                            tokenHashingService.hash(command.recoveryCode().trim().toUpperCase()))
                    .orElseThrow(InvalidTotpCodeException::new);
            recoveryCode.consume();
            authenticatedWithRecoveryCode = true;
        } else {
            throw new InvalidTotpCodeException();
        }

        challenge.complete();
        SessionTokenService.IssuedSession issuedSession = sessionTokenService.issueFor(user);

        auditEventRepository.save(AuditEvent.create(
                "USER",
                user.getId(),
                user.getEmail(),
                "USER_LOGGED_IN",
                "USER",
                user.getId(),
                null,
                "{\"sessionId\":\"" + issuedSession.sessionId()
                        + "\",\"authenticationMethod\":\"EMAIL_PASSWORD_MFA"
                        + "\",\"recoveryCodeUsed\":" + authenticatedWithRecoveryCode + "}"));

        return new CompletedLoginResult(
                user.getId(),
                issuedSession.sessionId(),
                issuedSession.accessToken(),
                issuedSession.accessTokenExpiresAt(),
                issuedSession.refreshToken(),
                issuedSession.refreshTokenExpiresAt(),
                true,
                authenticatedWithRecoveryCode);
    }

    private AuthMfaLoginChallenge loadPendingChallenge(String token) {
        AuthMfaLoginChallenge challenge = authMfaLoginChallengeRepository.findByToken(token.trim())
                .orElseThrow(MfaLoginChallengeNotFoundException::new);
        if (challenge.isExpiredAt(OffsetDateTime.now())) {
            challenge.expire();
            authMfaLoginChallengeRepository.save(challenge);
            throw new MfaLoginChallengeExpiredException();
        }
        if (!challenge.isPending()) {
            throw new MfaLoginChallengeAlreadyUsedException();
        }
        return challenge;
    }

    public record CompleteLoginChallengeCommand(
            @NotBlank String challengeToken,
            String totpCode,
            String recoveryCode) {
    }

    public record LoginChallengeResult(
            String challengeToken,
            OffsetDateTime expiresAt) {
    }

    public record CompletedLoginResult(
            UUID userId,
            UUID sessionId,
            String accessToken,
            OffsetDateTime accessTokenExpiresAt,
            String refreshToken,
            OffsetDateTime refreshTokenExpiresAt,
            boolean mfaSatisfied,
            boolean recoveryCodeUsed) {
    }
}
