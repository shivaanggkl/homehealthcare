package com.homehealthcare.auth.domain;

import com.homehealthcare.shared.persistence.AuditableEntity;
import com.homehealthcare.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auth_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "access_token_hash", nullable = false, length = 128)
    private String accessTokenHash;

    @Column(name = "access_token_expires_at", nullable = false)
    private OffsetDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private OffsetDateTime refreshTokenExpiresAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "absolute_expires_at", nullable = false)
    private OffsetDateTime absoluteExpiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revocation_reason", length = 64)
    private String revocationReason;

    @Builder
    private AuthSession(
            UUID id,
            User user,
            String accessTokenHash,
            OffsetDateTime accessTokenExpiresAt,
            String refreshTokenHash,
            OffsetDateTime refreshTokenExpiresAt,
            OffsetDateTime lastActivityAt,
            OffsetDateTime absoluteExpiresAt,
            OffsetDateTime revokedAt,
            String revocationReason) {
        this.id = id;
        this.user = user;
        this.accessTokenHash = accessTokenHash;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.lastActivityAt = lastActivityAt;
        this.absoluteExpiresAt = absoluteExpiresAt;
        this.revokedAt = revokedAt;
        this.revocationReason = revocationReason;
    }

    public static AuthSession issue(
            User user,
            String accessTokenHash,
            OffsetDateTime accessTokenExpiresAt,
            String refreshTokenHash,
            OffsetDateTime refreshTokenExpiresAt,
            OffsetDateTime lastActivityAt,
            OffsetDateTime absoluteExpiresAt) {
        return AuthSession.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .accessTokenHash(Objects.requireNonNull(accessTokenHash, "accessTokenHash must not be null"))
                .accessTokenExpiresAt(Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt must not be null"))
                .refreshTokenHash(Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null"))
                .refreshTokenExpiresAt(Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt must not be null"))
                .lastActivityAt(Objects.requireNonNull(lastActivityAt, "lastActivityAt must not be null"))
                .absoluteExpiresAt(Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt must not be null"))
                .build();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isAccessTokenExpiredAt(OffsetDateTime timestamp) {
        return !accessTokenExpiresAt.isAfter(timestamp);
    }

    public boolean isRefreshTokenExpiredAt(OffsetDateTime timestamp) {
        return !refreshTokenExpiresAt.isAfter(timestamp);
    }

    public boolean isIdleExpiredAt(OffsetDateTime timestamp, java.time.Duration idleTimeout) {
        return !lastActivityAt.plus(idleTimeout).isAfter(timestamp);
    }

    public boolean isAbsoluteExpiredAt(OffsetDateTime timestamp) {
        return !absoluteExpiresAt.isAfter(timestamp);
    }

    public OffsetDateTime idleTimeoutAt(java.time.Duration idleTimeout) {
        return lastActivityAt.plus(idleTimeout);
    }

    public OffsetDateTime forcedLogoutAt(java.time.Duration idleTimeout) {
        OffsetDateTime idleExpiresAt = idleTimeoutAt(idleTimeout);
        return idleExpiresAt.isBefore(absoluteExpiresAt) ? idleExpiresAt : absoluteExpiresAt;
    }

    public void recordActivity(OffsetDateTime occurredAt) {
        this.lastActivityAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public void revoke(String reason, OffsetDateTime revokedAt) {
        if (isRevoked()) {
            return;
        }
        this.revocationReason = Objects.requireNonNull(reason, "reason must not be null");
        this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
    }

    @PrePersist
    void initialize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthSession authSession)) {
            return false;
        }
        return id != null && Objects.equals(id, authSession.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
