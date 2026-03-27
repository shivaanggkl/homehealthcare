package com.homehealthcare.agency.domain;

import com.homehealthcare.shared.persistence.AuditableEntity;
import com.homehealthcare.security.branch.AgencyRole;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "agencies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agency extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank
    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AgencyStatus status;

    @NotBlank
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @NotBlank
    @Email
    @Column(name = "contact_email", nullable = false, length = 320)
    private String contactEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_policy_mode", nullable = false, length = 32)
    private AgencyMfaPolicyMode mfaPolicyMode;

    @Column(name = "mfa_required_roles", columnDefinition = "clob")
    private String mfaRequiredRoles;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    @Builder
    private Agency(
            UUID id,
            String name,
            String slug,
            AgencyStatus status,
            String timezone,
            String contactEmail,
            AgencyMfaPolicyMode mfaPolicyMode,
            String mfaRequiredRoles,
            OffsetDateTime deactivatedAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.status = status;
        this.timezone = timezone;
        this.contactEmail = contactEmail;
        this.mfaPolicyMode = mfaPolicyMode;
        this.mfaRequiredRoles = mfaRequiredRoles;
        this.deactivatedAt = deactivatedAt;
    }

    public static Agency create(String name, String slug, String timezone, String contactEmail) {
        return Agency.builder()
                .id(UUID.randomUUID())
                .name(name)
                .slug(slug)
                .status(AgencyStatus.ACTIVE)
                .timezone(timezone)
                .contactEmail(contactEmail)
                .mfaPolicyMode(AgencyMfaPolicyMode.OFF)
                .build();
    }

    public void disableMfaRequirement() {
        this.mfaPolicyMode = AgencyMfaPolicyMode.OFF;
        this.mfaRequiredRoles = null;
    }

    public void requireMfaForAllUsers() {
        this.mfaPolicyMode = AgencyMfaPolicyMode.ALL_USERS;
        this.mfaRequiredRoles = null;
    }

    public void requireMfaForRoles(Set<AgencyRole> roles) {
        Set<AgencyRole> normalizedRoles = new LinkedHashSet<>(Objects.requireNonNull(roles, "roles must not be null"));
        if (normalizedRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be configured for role-based MFA");
        }
        this.mfaPolicyMode = AgencyMfaPolicyMode.SELECTED_ROLES;
        this.mfaRequiredRoles = normalizedRoles.stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }

    public boolean requiresMfaForRole(AgencyRole role) {
        return switch (mfaPolicyMode) {
            case OFF -> false;
            case ALL_USERS -> true;
            case SELECTED_ROLES -> requiredMfaRoles().contains(role);
        };
    }

    public Set<AgencyRole> requiredMfaRoles() {
        if (mfaRequiredRoles == null || mfaRequiredRoles.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(mfaRequiredRoles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(AgencyRole::valueOf)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public void suspend() {
        this.status = AgencyStatus.SUSPENDED;
    }

    public void activate() {
        this.status = AgencyStatus.ACTIVE;
        this.deactivatedAt = null;
    }

    public void deactivate() {
        this.status = AgencyStatus.INACTIVE;
        this.deactivatedAt = OffsetDateTime.now();
    }

    public boolean isDeactivated() {
        return this.deactivatedAt != null;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = normalizeRequired(name);
        slug = normalizeSlug(slug);
        timezone = normalizeTimezone(timezone);
        contactEmail = normalizeEmail(contactEmail);
        if (mfaPolicyMode == null) {
            mfaPolicyMode = AgencyMfaPolicyMode.OFF;
        }
        if (mfaPolicyMode != AgencyMfaPolicyMode.SELECTED_ROLES) {
            mfaRequiredRoles = null;
        }
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSlug(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeTimezone(String value) {
        String normalized = normalizeRequired(value);
        if (normalized == null || normalized.isBlank()) {
            return normalized;
        }
        ZoneId.of(normalized);
        return normalized;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void updateSlug(String newSlug) {
        this.slug = newSlug;
    }

    public void updateTimezone(String newTimezone) {
        this.timezone = newTimezone;
    }

    public void updateContactEmail(String newContactEmail) {
        this.contactEmail = newContactEmail;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Agency agency)) {
            return false;
        }
        return id != null && Objects.equals(id, agency.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
