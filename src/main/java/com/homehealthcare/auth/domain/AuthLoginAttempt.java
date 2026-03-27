package com.homehealthcare.auth.domain;

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
@Table(name = "auth_login_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthLoginAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private AuthLoginAttemptOutcome outcome;

    @Column(name = "failure_reason", length = 64)
    private String failureReason;

    @NotNull
    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt;

    @Builder
    private AuthLoginAttempt(
            UUID id,
            User user,
            String email,
            String ipAddress,
            AuthLoginAttemptOutcome outcome,
            String failureReason,
            OffsetDateTime attemptedAt) {
        this.id = id;
        this.user = user;
        this.email = email;
        this.ipAddress = ipAddress;
        this.outcome = outcome;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }

    public static AuthLoginAttempt record(
            User user,
            String email,
            String ipAddress,
            AuthLoginAttemptOutcome outcome,
            String failureReason,
            OffsetDateTime attemptedAt) {
        return AuthLoginAttempt.builder()
                .id(UUID.randomUUID())
                .user(user)
                .email(email)
                .ipAddress(ipAddress)
                .outcome(outcome)
                .failureReason(failureReason)
                .attemptedAt(Objects.requireNonNull(attemptedAt, "attemptedAt must not be null"))
                .build();
    }

    public boolean isFailure() {
        return outcome == AuthLoginAttemptOutcome.FAILURE;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        ipAddress = ipAddress == null || ipAddress.isBlank() ? null : ipAddress.trim();
        failureReason = failureReason == null || failureReason.isBlank() ? null : failureReason.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuthLoginAttempt attempt)) {
            return false;
        }
        return id != null && Objects.equals(id, attempt.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
