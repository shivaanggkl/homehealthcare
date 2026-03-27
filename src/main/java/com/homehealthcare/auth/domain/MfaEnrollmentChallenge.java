package com.homehealthcare.auth.domain;

import com.homehealthcare.shared.persistence.AuditableEntity;
import com.homehealthcare.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "mfa_enrollment_challenges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfaEnrollmentChallenge extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_session_id", nullable = false, updatable = false)
    private AuthSession authSession;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "totp_secret", nullable = false, length = 64)
    private String totpSecret;

    @Column(name = "recovery_code_hashes", nullable = false, columnDefinition = "clob")
    private String recoveryCodeHashes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MfaEnrollmentChallengeStatus status;

    @Column(name = "reauthenticated_at", nullable = false)
    private OffsetDateTime reauthenticatedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Builder
    private MfaEnrollmentChallenge(
            UUID id,
            User user,
            AuthSession authSession,
            String token,
            String totpSecret,
            String recoveryCodeHashes,
            MfaEnrollmentChallengeStatus status,
            OffsetDateTime reauthenticatedAt,
            OffsetDateTime expiresAt,
            OffsetDateTime completedAt,
            OffsetDateTime cancelledAt) {
        this.id = id;
        this.user = user;
        this.authSession = authSession;
        this.token = token;
        this.totpSecret = totpSecret;
        this.recoveryCodeHashes = recoveryCodeHashes;
        this.status = status;
        this.reauthenticatedAt = reauthenticatedAt;
        this.expiresAt = expiresAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
    }

    public static MfaEnrollmentChallenge issue(
            User user,
            AuthSession authSession,
            String token,
            String totpSecret,
            List<String> recoveryCodeHashes,
            OffsetDateTime expiresAt) {
        OffsetDateTime reauthenticatedAt = OffsetDateTime.now();
        return MfaEnrollmentChallenge.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .authSession(Objects.requireNonNull(authSession, "authSession must not be null"))
                .token(Objects.requireNonNull(token, "token must not be null"))
                .totpSecret(Objects.requireNonNull(totpSecret, "totpSecret must not be null"))
                .recoveryCodeHashes(String.join("\n", Objects.requireNonNull(recoveryCodeHashes, "recoveryCodeHashes must not be null")))
                .status(MfaEnrollmentChallengeStatus.PENDING)
                .reauthenticatedAt(reauthenticatedAt)
                .expiresAt(Objects.requireNonNull(expiresAt, "expiresAt must not be null"))
                .build();
    }

    public List<String> recoveryCodeHashes() {
        return Arrays.stream(recoveryCodeHashes.split("\\n"))
                .filter(value -> !value.isBlank())
                .toList();
    }

    public void complete() {
        this.status = MfaEnrollmentChallengeStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void cancel() {
        this.status = MfaEnrollmentChallengeStatus.CANCELLED;
        this.cancelledAt = OffsetDateTime.now();
    }

    public void expire() {
        this.status = MfaEnrollmentChallengeStatus.EXPIRED;
    }

    public void rescheduleExpiration(OffsetDateTime expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public boolean isPending() {
        return status == MfaEnrollmentChallengeStatus.PENDING;
    }

    public boolean isExpiredAt(OffsetDateTime timestamp) {
        return expiresAt.isBefore(timestamp) || expiresAt.isEqual(timestamp);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        token = token == null ? null : token.trim();
        totpSecret = totpSecret == null ? null : totpSecret.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MfaEnrollmentChallenge challenge)) {
            return false;
        }
        return id != null && Objects.equals(id, challenge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
