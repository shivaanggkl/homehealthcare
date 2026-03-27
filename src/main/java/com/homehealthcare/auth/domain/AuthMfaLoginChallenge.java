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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auth_mfa_login_challenges")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthMfaLoginChallenge extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AuthMfaLoginChallengeStatus status;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Builder
    private AuthMfaLoginChallenge(
            UUID id,
            User user,
            String email,
            String token,
            OffsetDateTime expiresAt,
            AuthMfaLoginChallengeStatus status,
            OffsetDateTime completedAt,
            OffsetDateTime cancelledAt) {
        this.id = id;
        this.user = user;
        this.email = email;
        this.token = token;
        this.expiresAt = expiresAt;
        this.status = status;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
    }

    public static AuthMfaLoginChallenge issue(User user, String email, String token, OffsetDateTime expiresAt) {
        return AuthMfaLoginChallenge.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .email(Objects.requireNonNull(email, "email must not be null"))
                .token(Objects.requireNonNull(token, "token must not be null"))
                .expiresAt(Objects.requireNonNull(expiresAt, "expiresAt must not be null"))
                .status(AuthMfaLoginChallengeStatus.PENDING)
                .build();
    }

    public void complete() {
        this.status = AuthMfaLoginChallengeStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void cancel() {
        this.status = AuthMfaLoginChallengeStatus.CANCELLED;
        this.cancelledAt = OffsetDateTime.now();
    }

    public void expire() {
        this.status = AuthMfaLoginChallengeStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == AuthMfaLoginChallengeStatus.PENDING;
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
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        token = token == null ? null : token.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthMfaLoginChallenge challenge)) {
            return false;
        }
        return id != null && Objects.equals(id, challenge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
