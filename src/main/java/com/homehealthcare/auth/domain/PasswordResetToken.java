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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "password_reset_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @NotBlank
    @Column(name = "token", nullable = false, unique = true, length = 128)
    private String token;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PasswordResetTokenStatus status;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @NotNull
    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Builder
    private PasswordResetToken(
            UUID id,
            User user,
            String email,
            String token,
            PasswordResetTokenStatus status,
            OffsetDateTime expiresAt,
            OffsetDateTime requestedAt,
            OffsetDateTime cancelledAt,
            OffsetDateTime consumedAt) {
        this.id = id;
        this.user = user;
        this.email = email;
        this.token = token;
        this.status = status;
        this.expiresAt = expiresAt;
        this.requestedAt = requestedAt;
        this.cancelledAt = cancelledAt;
        this.consumedAt = consumedAt;
    }

    public static PasswordResetToken issue(User user, String email, String token, OffsetDateTime expiresAt) {
        OffsetDateTime requestedAt = OffsetDateTime.now();
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .email(Objects.requireNonNull(email, "email must not be null"))
                .token(Objects.requireNonNull(token, "token must not be null"))
                .status(PasswordResetTokenStatus.PENDING)
                .expiresAt(Objects.requireNonNull(expiresAt, "expiresAt must not be null"))
                .requestedAt(requestedAt)
                .build();
    }

    public void cancel() {
        this.status = PasswordResetTokenStatus.CANCELLED;
        this.cancelledAt = OffsetDateTime.now();
    }

    public void consume() {
        this.status = PasswordResetTokenStatus.CONSUMED;
        this.consumedAt = OffsetDateTime.now();
    }

    public void expire() {
        this.status = PasswordResetTokenStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == PasswordResetTokenStatus.PENDING;
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
        if (!(other instanceof PasswordResetToken resetToken)) {
            return false;
        }
        return id != null && Objects.equals(id, resetToken.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
