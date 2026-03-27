package com.homehealthcare.user.domain;

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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

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

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "preferred_language", length = 35)
    private String preferredLanguage;

    @Column(name = "time_zone", length = 64)
    private String timeZone;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    @Column(name = "mfa_enrolled_at")
    private OffsetDateTime mfaEnrolledAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    @Builder
    private User(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String preferredLanguage,
            String timeZone,
            String passwordHash,
            boolean mfaEnabled,
            String mfaSecret,
            OffsetDateTime mfaEnrolledAt,
            UserStatus status,
            OffsetDateTime lastLoginAt,
            OffsetDateTime deactivatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.preferredLanguage = preferredLanguage;
        this.timeZone = timeZone;
        this.passwordHash = passwordHash;
        this.mfaEnabled = mfaEnabled;
        this.mfaSecret = mfaSecret;
        this.mfaEnrolledAt = mfaEnrolledAt;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
        this.deactivatedAt = deactivatedAt;
    }

    public static User invite(String firstName, String lastName, String email, String phone) {
        return User.builder()
                .id(UUID.randomUUID())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .status(UserStatus.INVITED)
                .build();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.deactivatedAt = null;
    }

    public void activateWithCredentials(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        activate();
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void deactivate() {
        this.status = UserStatus.DEACTIVATED;
        this.deactivatedAt = OffsetDateTime.now();
    }

    public boolean isDeactivated() {
        return deactivatedAt != null;
    }

    public void recordLogin(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = Objects.requireNonNull(lastLoginAt, "lastLoginAt must not be null");
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public void updateSelfProfile(String firstName, String lastName, String phone, String preferredLanguage, String timeZone) {
        this.firstName = normalizeRequired(firstName);
        this.lastName = normalizeRequired(lastName);
        this.phone = normalizeOptional(phone);
        this.preferredLanguage = normalizeLanguageTag(preferredLanguage);
        this.timeZone = normalizeTimezone(timeZone);
    }

    public boolean hasPasswordHash() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public void enableMfa() {
        this.mfaEnabled = true;
    }

    public void enrollMfa(String mfaSecret, OffsetDateTime enrolledAt) {
        this.mfaSecret = normalizeOptional(mfaSecret);
        this.mfaEnrolledAt = Objects.requireNonNull(enrolledAt, "enrolledAt must not be null");
        this.mfaEnabled = true;
    }

    public void disableMfa() {
        this.mfaEnabled = false;
        this.mfaSecret = null;
        this.mfaEnrolledAt = null;
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
        phone = normalizeOptional(phone);
        preferredLanguage = normalizeLanguageTag(preferredLanguage);
        timeZone = normalizeTimezone(timeZone);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeLanguageTag(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("(?i)^[a-z]{2,3}(-[a-z]{2})?$")) {
            throw new IllegalArgumentException("Invalid preferred language tag");
        }
        Locale locale = Locale.forLanguageTag(normalized);
        return locale.toLanguageTag();
    }

    private static String normalizeTimezone(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        ZoneId.of(normalized);
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
