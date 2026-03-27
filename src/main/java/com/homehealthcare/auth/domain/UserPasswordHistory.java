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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_password_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPasswordHistory extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @Builder
    private UserPasswordHistory(UUID id, User user, String passwordHash, OffsetDateTime recordedAt) {
        this.id = id;
        this.user = user;
        this.passwordHash = passwordHash;
        this.recordedAt = recordedAt;
    }

    public static UserPasswordHistory record(User user, String passwordHash, OffsetDateTime recordedAt) {
        return UserPasswordHistory.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .passwordHash(Objects.requireNonNull(passwordHash, "passwordHash must not be null"))
                .recordedAt(Objects.requireNonNull(recordedAt, "recordedAt must not be null"))
                .build();
    }

    @PrePersist
    void ensureId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserPasswordHistory that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
