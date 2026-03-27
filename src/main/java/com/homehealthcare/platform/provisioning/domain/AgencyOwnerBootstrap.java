package com.homehealthcare.platform.provisioning.domain;

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
@Table(name = "agency_owner_bootstraps")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyOwnerBootstrap extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "owner_first_name", nullable = false, length = 100)
    private String ownerFirstName;

    @NotBlank
    @Column(name = "owner_last_name", nullable = false, length = 100)
    private String ownerLastName;

    @NotBlank
    @Email
    @Column(name = "owner_email", nullable = false, length = 320)
    private String ownerEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AgencyOwnerBootstrapStatus status;

    @Builder
    private AgencyOwnerBootstrap(
            UUID id,
            Agency agency,
            String ownerFirstName,
            String ownerLastName,
            String ownerEmail,
            AgencyOwnerBootstrapStatus status) {
        this.id = id;
        assignAgency(agency);
        this.ownerFirstName = ownerFirstName;
        this.ownerLastName = ownerLastName;
        this.ownerEmail = ownerEmail;
        this.status = status;
    }

    public static AgencyOwnerBootstrap create(Agency agency, String ownerFirstName, String ownerLastName, String ownerEmail) {
        return AgencyOwnerBootstrap.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .ownerFirstName(ownerFirstName)
                .ownerLastName(ownerLastName)
                .ownerEmail(ownerEmail)
                .status(AgencyOwnerBootstrapStatus.PENDING)
                .build();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        ownerFirstName = normalizeRequired(ownerFirstName);
        ownerLastName = normalizeRequired(ownerLastName);
        ownerEmail = normalizeEmail(ownerEmail);
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
        if (!(other instanceof AgencyOwnerBootstrap agencyOwnerBootstrap)) {
            return false;
        }
        return id != null && Objects.equals(id, agencyOwnerBootstrap.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
