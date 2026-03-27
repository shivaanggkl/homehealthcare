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
            OffsetDateTime revokedAt,
            String revocationReason) {
        this.id = id;
        this.user = user;
        this.accessTokenHash = accessTokenHash;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.revokedAt = revokedAt;
        this.revocationReason = revocationReason;
    }

    public static AuthSession issue(
            User user,
            String accessTokenHash,
            OffsetDateTime accessTokenExpiresAt,
            String refreshTokenHash,
            OffsetDateTime refreshTokenExpiresAt) {
        return AuthSession.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .accessTokenHash(Objects.requireNonNull(accessTokenHash, "accessTokenHash must not be null"))
                .accessTokenExpiresAt(Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt must not be null"))
                .refreshTokenHash(Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null"))
                .refreshTokenExpiresAt(Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt must not be null"))
                .build();
    }

    public boolean isRevoked() {
        return revokedAt != null;
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
