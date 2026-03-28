package com.homehealthcare.certification.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_certifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverCertification extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "expiration_required", nullable = false)
    private boolean expirationRequired;

    @Builder
    private CaregiverCertification(
            UUID id,
            Agency agency,
            String name,
            String code,
            String description,
            boolean expirationRequired) {
        this.id = id;
        assignAgency(agency);
        this.name = name;
        this.code = code;
        this.description = description;
        this.expirationRequired = expirationRequired;
    }

    public static CaregiverCertification create(
            Agency agency,
            String name,
            String code,
            String description,
            boolean expirationRequired) {
        CaregiverCertification certification = CaregiverCertification.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name(name)
                .code(code)
                .description(description)
                .expirationRequired(expirationRequired)
                .build();
        certification.activate();
        return certification;
    }

    public void updateDetails(String name, String code, String description, boolean expirationRequired) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.expirationRequired = expirationRequired;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = Objects.requireNonNull(name, "name must not be null").trim();
        code = Objects.requireNonNull(code, "code must not be null").trim().toUpperCase(Locale.ROOT);
        description = description == null || description.trim().isBlank() ? null : description.trim();
    }
}
