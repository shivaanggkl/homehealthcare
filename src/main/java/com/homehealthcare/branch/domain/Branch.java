package com.homehealthcare.branch.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
@Table(name = "branches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotBlank
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @NotBlank
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BranchStatus status;

    @Column(name = "deactivated_at")
    private OffsetDateTime deactivatedAt;

    @Builder
    private Branch(
            UUID id,
            Agency agency,
            String name,
            String code,
            String address,
            String timezone,
            BranchStatus status,
            OffsetDateTime deactivatedAt) {
        this.id = id;
        assignAgency(agency);
        this.name = name;
        this.code = code;
        this.address = address;
        this.timezone = timezone;
        this.status = status;
        this.deactivatedAt = deactivatedAt;
    }

    public static Branch create(Agency agency, String name, String code, String address, String timezone) {
        return Branch.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name(name)
                .code(code)
                .address(address)
                .timezone(timezone)
                .status(BranchStatus.ACTIVE)
                .build();
    }

    public void activate() {
        this.status = BranchStatus.ACTIVE;
        this.deactivatedAt = null;
    }

    public void deactivate() {
        this.status = BranchStatus.INACTIVE;
        this.deactivatedAt = OffsetDateTime.now();
    }

    public boolean isDeactivated() {
        return deactivatedAt != null;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = normalizeRequired(name);
        code = normalizeCode(code);
        address = normalizeRequired(address);
        timezone = normalizeTimezone(timezone);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeCode(String value) {
        String normalized = normalizeRequired(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
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

    public void updateCode(String newCode) {
        this.code = newCode;
    }

    public void updateAddress(String newAddress) {
        this.address = newAddress;
    }

    public void updateTimezone(String newTimezone) {
        this.timezone = newTimezone;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Branch branch)) {
            return false;
        }
        return id != null && Objects.equals(id, branch.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
