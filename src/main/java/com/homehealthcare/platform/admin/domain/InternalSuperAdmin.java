package com.homehealthcare.platform.admin.domain;

import com.homehealthcare.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "internal_super_admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InternalSuperAdmin extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InternalSuperAdminStatus status;

    @Builder
    private InternalSuperAdmin(
            UUID id,
            String firstName,
            String lastName,
            String email,
            InternalSuperAdminStatus status) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
    }

    public static InternalSuperAdmin create(String firstName, String lastName, String email) {
        return InternalSuperAdmin.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .status(InternalSuperAdminStatus.ACTIVE)
                .build();
    }

    public boolean isActive() {
        return status == InternalSuperAdminStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = InternalSuperAdminStatus.INACTIVE;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        firstName = normalizeRequired(firstName);
        lastName = normalizeRequired(lastName);
        email = normalizeEmail(email);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InternalSuperAdmin internalSuperAdmin)) {
            return false;
        }
        return id != null && Objects.equals(id, internalSuperAdmin.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
