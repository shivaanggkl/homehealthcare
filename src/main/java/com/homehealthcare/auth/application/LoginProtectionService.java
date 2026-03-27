package com.homehealthcare.auth.application;

import com.homehealthcare.auth.domain.AuthLoginAttempt;
import com.homehealthcare.auth.domain.AuthLoginAttemptOutcome;
import com.homehealthcare.auth.domain.AuthLoginAttemptRepository;
import com.homehealthcare.user.domain.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginProtectionService {

    private static final List<AuthLoginAttemptOutcome> FAILURE_OUTCOMES = List.of(AuthLoginAttemptOutcome.FAILURE);

    private final AuthLoginAttemptRepository authLoginAttemptRepository;
    private final LoginProtectionProperties properties;

    @Transactional
    public void assertLoginAllowed(String email, String ipAddress) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedIp = normalizeIp(ipAddress);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime rateLimitCutoff = now.minus(properties.getRateLimitWindow());

        if (normalizedIp != null
                && authLoginAttemptRepository.countByIpAddressAndAttemptedAtAfter(normalizedIp, rateLimitCutoff)
                >= properties.getMaxAttemptsPerIp()) {
            recordAttempt(null, normalizedEmail, normalizedIp, AuthLoginAttemptOutcome.RATE_LIMITED, "IP_RATE_LIMITED", now);
            log.warn("Rate limited login attempt by ip={} email={}", normalizedIp, normalizedEmail);
            throw new TooManyLoginAttemptsException();
        }

        if (authLoginAttemptRepository.countByEmailAndAttemptedAtAfter(normalizedEmail, rateLimitCutoff)
                >= properties.getMaxAttemptsPerEmail()) {
            recordAttempt(null, normalizedEmail, normalizedIp, AuthLoginAttemptOutcome.RATE_LIMITED, "EMAIL_RATE_LIMITED", now);
            log.warn("Rate limited login attempt by email={} ip={}", normalizedEmail, normalizedIp);
            throw new TooManyLoginAttemptsException();
        }

        OffsetDateTime lockoutUntil = currentLockoutUntil(normalizedEmail, now);
        if (lockoutUntil != null && lockoutUntil.isAfter(now)) {
            recordAttempt(null, normalizedEmail, normalizedIp, AuthLoginAttemptOutcome.LOCKED_OUT, "TEMPORARY_LOCKOUT", now);
            log.warn("Temporarily locked out login email={} ip={} until={}", normalizedEmail, normalizedIp, lockoutUntil);
            throw new TooManyLoginAttemptsException();
        }
    }

    @Transactional
    public void recordFailure(String email, String ipAddress, User user, String reason) {
        if (!properties.isEnabled()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String normalizedEmail = normalizeEmail(email);
        String normalizedIp = normalizeIp(ipAddress);
        recordAttempt(user, normalizedEmail, normalizedIp, AuthLoginAttemptOutcome.FAILURE, reason, now);

        OffsetDateTime lockoutUntil = currentLockoutUntil(normalizedEmail, now);
        if (lockoutUntil != null && lockoutUntil.isAfter(now)) {
            log.warn("Temporary login lockout started email={} ip={} until={} reason={}",
                    normalizedEmail,
                    normalizedIp,
                    lockoutUntil,
                    reason);
        } else {
            log.warn("Recorded failed login email={} ip={} reason={}", normalizedEmail, normalizedIp, reason);
        }
    }

    @Transactional
    public void recordSuccess(String email, String ipAddress, User user) {
        if (!properties.isEnabled()) {
            return;
        }

        recordAttempt(
                user,
                normalizeEmail(email),
                normalizeIp(ipAddress),
                AuthLoginAttemptOutcome.SUCCESS,
                null,
                OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public OffsetDateTime currentLockoutUntil(String email, OffsetDateTime now) {
        if (!properties.isEnabled()) {
            return null;
        }

        String normalizedEmail = normalizeEmail(email);
        OffsetDateTime failureCutoff = now.minus(properties.getFailureWindow());
        OffsetDateTime baseline = authLoginAttemptRepository.findFirstByEmailAndOutcomeOrderByAttemptedAtDesc(
                        normalizedEmail,
                        AuthLoginAttemptOutcome.SUCCESS)
                .map(AuthLoginAttempt::getAttemptedAt)
                .map(successTime -> successTime.isAfter(failureCutoff) ? successTime : failureCutoff)
                .orElse(failureCutoff);

        long consecutiveFailures = authLoginAttemptRepository.countByEmailAndOutcomeInAndAttemptedAtAfter(
                normalizedEmail,
                FAILURE_OUTCOMES,
                baseline);

        if (consecutiveFailures < properties.getFailureThreshold()) {
            return null;
        }

        return authLoginAttemptRepository.findFirstByEmailAndOutcomeInOrderByAttemptedAtDesc(
                        normalizedEmail,
                        FAILURE_OUTCOMES)
                .map(AuthLoginAttempt::getAttemptedAt)
                .map(lastFailure -> lastFailure.plus(properties.getTemporaryLockoutDuration()))
                .orElse(null);
    }

    private void recordAttempt(
            User user,
            String email,
            String ipAddress,
            AuthLoginAttemptOutcome outcome,
            String reason,
            OffsetDateTime attemptedAt) {
        authLoginAttemptRepository.save(AuthLoginAttempt.record(
                user,
                email,
                ipAddress,
                outcome,
                reason,
                attemptedAt));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIp(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        String normalized = ipAddress.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
