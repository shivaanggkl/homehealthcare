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
@Table(name = "user_mfa_recovery_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMfaRecoveryCode extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "code_hash", nullable = false, unique = true, length = 128)
    private String codeHash;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Builder
    private UserMfaRecoveryCode(UUID id, User user, String codeHash, int ordinal, OffsetDateTime consumedAt) {
        this.id = id;
        this.user = user;
        this.codeHash = codeHash;
        this.ordinal = ordinal;
        this.consumedAt = consumedAt;
    }

    public static UserMfaRecoveryCode issue(User user, String codeHash, int ordinal) {
        return UserMfaRecoveryCode.builder()
                .id(UUID.randomUUID())
                .user(Objects.requireNonNull(user, "user must not be null"))
                .codeHash(Objects.requireNonNull(codeHash, "codeHash must not be null"))
                .ordinal(ordinal)
                .build();
    }

    public void consume() {
        this.consumedAt = OffsetDateTime.now();
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
        if (!(other instanceof UserMfaRecoveryCode recoveryCode)) {
            return false;
        }
        return id != null && Objects.equals(id, recoveryCode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
