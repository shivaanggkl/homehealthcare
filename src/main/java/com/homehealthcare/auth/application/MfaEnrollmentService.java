package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.auth.domain.MfaEnrollmentChallenge;
import com.homehealthcare.auth.domain.MfaEnrollmentChallengeRepository;
import com.homehealthcare.auth.domain.MfaEnrollmentChallengeStatus;
import com.homehealthcare.auth.domain.UserMfaRecoveryCode;
import com.homehealthcare.auth.domain.UserMfaRecoveryCodeRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class MfaEnrollmentService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ACTION_MFA_ENROLLMENT_STARTED = "MFA_ENROLLMENT_STARTED";
    private static final String ACTION_MFA_ENROLLMENT_FAILED = "MFA_ENROLLMENT_FAILED";
    private static final String ACTION_MFA_ENROLLED = "MFA_ENROLLED";

    private final CurrentAuthSessionResolver currentAuthSessionResolver;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final MfaProperties mfaProperties;
    private final TokenHashingService tokenHashingService;
    private final MfaEnrollmentChallengeRepository mfaEnrollmentChallengeRepository;
    private final UserMfaRecoveryCodeRepository userMfaRecoveryCodeRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public EnrollmentStartResult startEnrollment(@Valid EnrollmentStartCommand command) {
        AuthSession currentSession = currentAuthSessionResolver.requireActive(
                command.accessToken(),
                command.sessionId());
        User user = currentSession.getUser();

        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new CurrentPasswordMismatchException();
        }

        mfaEnrollmentChallengeRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        MfaEnrollmentChallengeStatus.PENDING)
                .ifPresent(existing -> {
                    existing.cancel();
                    mfaEnrollmentChallengeRepository.save(existing);
                });

        String secret = totpService.generateSecret();
        List<String> recoveryCodes = generateRecoveryCodes(mfaProperties.getRecoveryCodeCount());
        List<String> recoveryCodeHashes = recoveryCodes.stream()
                .map(tokenHashingService::hash)
                .toList();

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(mfaProperties.getEnrollmentChallengeTtl());
        MfaEnrollmentChallenge challenge = mfaEnrollmentChallengeRepository.save(MfaEnrollmentChallenge.issue(
                user,
                currentSession,
                UUID.randomUUID().toString(),
                secret,
                recoveryCodeHashes,
                expiresAt));

        auditEventRepository.save(AuditEvent.createSuccess(
                "USER",
                user.getId(),
                user.getEmail(),
                ACTION_MFA_ENROLLMENT_STARTED,
                "MFA_ENROLLMENT_CHALLENGE",
                challenge.getId(),
                null,
                null,
                "{\"expiresAt\":\"" + expiresAt
                        + "\",\"recoveryCodeCount\":" + recoveryCodes.size() + "}"));

        return new EnrollmentStartResult(
                challenge.getToken(),
                secret,
                totpService.buildOtpauthUri(mfaProperties.getIssuer(), user.getEmail(), secret),
                expiresAt,
                recoveryCodes);
    }

    @Transactional
    public EnrollmentConfirmResult confirmEnrollment(@Valid EnrollmentConfirmCommand command) {
        MfaEnrollmentChallenge challenge = loadPendingChallenge(command.enrollmentToken());
        if (!totpService.verifyCode(challenge.getTotpSecret(), command.totpCode(), Instant.now())) {
            auditEventRepository.save(AuditEvent.createFailure(
                    "USER",
                    challenge.getUser().getId(),
                    challenge.getUser().getEmail(),
                    ACTION_MFA_ENROLLMENT_FAILED,
                    "MFA_ENROLLMENT_CHALLENGE",
                    challenge.getId(),
                    null,
                    null,
                    "{\"reason\":\"INVALID_TOTP_CODE\"}"));
            throw new InvalidTotpCodeException();
        }

        User user = challenge.getUser();
        user.enrollMfa(challenge.getTotpSecret(), OffsetDateTime.now());
        userMfaRecoveryCodeRepository.deleteAllByUser_Id(user.getId());

        List<String> recoveryCodeHashes = challenge.recoveryCodeHashes();
        for (int index = 0; index < recoveryCodeHashes.size(); index++) {
            userMfaRecoveryCodeRepository.save(UserMfaRecoveryCode.issue(user, recoveryCodeHashes.get(index), index));
        }

        challenge.complete();

        auditEventRepository.save(AuditEvent.createSuccess(
                "USER",
                user.getId(),
                user.getEmail(),
                ACTION_MFA_ENROLLED,
                "USER",
                user.getId(),
                null,
                null,
                "{\"recoveryCodeCount\":" + recoveryCodeHashes.size() + "}"));

        return new EnrollmentConfirmResult(
                user.getId(),
                true,
                userMfaRecoveryCodeRepository.countByUser_IdAndConsumedAtIsNull(user.getId()));
    }

    @Transactional(readOnly = true)
    public MfaStatusView currentStatus(@Valid MfaStatusCommand command) {
        AuthSession currentSession = currentAuthSessionResolver.requireActive(
                command.accessToken(),
                command.sessionId());
        User user = currentSession.getUser();
        return new MfaStatusView(
                user.getId(),
                user.isMfaEnabled(),
                user.getMfaEnrolledAt(),
                userMfaRecoveryCodeRepository.countByUser_IdAndConsumedAtIsNull(user.getId()));
    }

    private MfaEnrollmentChallenge loadPendingChallenge(String token) {
        MfaEnrollmentChallenge challenge = mfaEnrollmentChallengeRepository.findByToken(token.trim())
                .orElseThrow(MfaEnrollmentChallengeNotFoundException::new);

        if (challenge.isExpiredAt(OffsetDateTime.now())) {
            challenge.expire();
            mfaEnrollmentChallengeRepository.save(challenge);
            throw new MfaEnrollmentChallengeExpiredException();
        }

        if (!challenge.isPending()) {
            throw new MfaEnrollmentChallengeAlreadyUsedException();
        }

        return challenge;
    }

    private static List<String> generateRecoveryCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte[] bytes = new byte[5];
            SECURE_RANDOM.nextBytes(bytes);
            String raw = java.util.HexFormat.of().formatHex(bytes).toUpperCase();
            codes.add(raw.substring(0, 4) + "-" + raw.substring(4, 8));
        }
        return codes;
    }

    public record EnrollmentStartCommand(
            String accessToken,
            String sessionId,
            @NotBlank String currentPassword) {
    }

    public record EnrollmentConfirmCommand(
            @NotBlank String enrollmentToken,
            @NotBlank String totpCode) {
    }

    public record MfaStatusCommand(
            String accessToken,
            String sessionId) {
    }

    public record EnrollmentStartResult(
            String enrollmentToken,
            String manualEntryKey,
            String otpauthUri,
            OffsetDateTime expiresAt,
            List<String> recoveryCodes) {
    }

    public record EnrollmentConfirmResult(
            UUID userId,
            boolean mfaEnabled,
            long recoveryCodesRemaining) {
    }

    public record MfaStatusView(
            UUID userId,
            boolean mfaEnabled,
            OffsetDateTime enrolledAt,
            long recoveryCodesRemaining) {
    }
}
