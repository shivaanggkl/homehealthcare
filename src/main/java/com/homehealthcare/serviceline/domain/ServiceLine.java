package com.homehealthcare.serviceline.domain;

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
@Table(name = "service_lines")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceLine extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder
    private ServiceLine(UUID id, Agency agency, String name, String code, String description) {
        this.id = id;
        assignAgency(agency);
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public static ServiceLine create(Agency agency, String name, String code, String description, int displayOrder) {
        ServiceLine serviceLine = ServiceLine.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name(name)
                .code(code)
                .description(description)
                .build();
        serviceLine.updateDisplayOrder(displayOrder);
        serviceLine.activate();
        return serviceLine;
    }

    public void updateDetails(String name, String code, String description, int displayOrder) {
        this.name = name;
        this.code = code;
        this.description = description;
        updateDisplayOrder(displayOrder);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = normalizeRequired(name);
        code = normalizeCode(code);
        description = normalizeOptional(description);
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeCode(String value) {
        return normalizeRequired(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
