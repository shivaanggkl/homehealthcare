package com.homehealthcare.visittype.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
import com.homehealthcare.serviceline.domain.ServiceLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "visit_types")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitType extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "default_duration_minutes", nullable = false)
    private int defaultDurationMinutes;

    @Column(name = "billable", nullable = false)
    private boolean billable;

    @Builder
    private VisitType(
            UUID id,
            Agency agency,
            ServiceLine serviceLine,
            String name,
            String code,
            String description,
            int defaultDurationMinutes,
            boolean billable) {
        this.id = id;
        assignAgency(agency);
        this.serviceLine = serviceLine;
        this.name = name;
        this.code = code;
        this.description = description;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.billable = billable;
    }

    public static VisitType create(
            Agency agency,
            ServiceLine serviceLine,
            String name,
            String code,
            String description,
            int defaultDurationMinutes,
            boolean billable,
            int displayOrder) {
        assertServiceLineAgency(agency, serviceLine);
        VisitType visitType = VisitType.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .serviceLine(serviceLine)
                .name(name)
                .code(code)
                .description(description)
                .defaultDurationMinutes(defaultDurationMinutes)
                .billable(billable)
                .build();
        visitType.updateDisplayOrder(displayOrder);
        visitType.activate();
        return visitType;
    }

    public void updateDetails(
            ServiceLine serviceLine,
            String name,
            String code,
            String description,
            int defaultDurationMinutes,
            boolean billable,
            int displayOrder) {
        assertServiceLineAgency(getAgency(), serviceLine);
        this.serviceLine = serviceLine;
        this.name = name;
        this.code = code;
        this.description = description;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.billable = billable;
        updateDisplayOrder(displayOrder);
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = normalizeRequired(name);
        code = normalizeRequired(code).toUpperCase(Locale.ROOT);
        description = normalizeOptional(description);
        if (defaultDurationMinutes <= 0) {
            throw new IllegalArgumentException("defaultDurationMinutes must be greater than 0");
        }
        assertServiceLineAgency(getAgency(), serviceLine);
    }

    private static void assertServiceLineAgency(Agency agency, ServiceLine serviceLine) {
        if (serviceLine == null) {
            return;
        }
        UUID agencyId = agency == null ? null : agency.getId();
        if (agencyId == null || !agencyId.equals(serviceLine.getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the visit type");
        }
    }

    private static String normalizeRequired(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
