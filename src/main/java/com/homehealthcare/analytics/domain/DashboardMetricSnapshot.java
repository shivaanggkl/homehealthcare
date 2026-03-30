package com.homehealthcare.analytics.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dashboard_metric_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardMetricSnapshot extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metric_definition_id", nullable = false)
    private DashboardMetricDefinition metricDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "primary_value", nullable = false)
    private int primaryValue;

    @Column(name = "secondary_value", nullable = false)
    private int secondaryValue;

    @Column(name = "status_code", length = 64)
    private String statusCode;

    @Column(name = "drilldown_reference_json", nullable = false, length = 4000)
    private String drilldownReferenceJson;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Builder
    private DashboardMetricSnapshot(
            UUID id,
            DashboardMetricDefinition metricDefinition,
            Branch branch,
            LocalDate snapshotDate,
            int primaryValue,
            int secondaryValue,
            String statusCode,
            String drilldownReferenceJson,
            OffsetDateTime capturedAt) {
        this.id = id;
        assignMetricDefinition(metricDefinition);
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.statusCode = statusCode;
        this.drilldownReferenceJson = drilldownReferenceJson;
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        validateState();
    }

    public static DashboardMetricSnapshot create(
            DashboardMetricDefinition metricDefinition,
            Branch branch,
            LocalDate snapshotDate,
            int primaryValue,
            int secondaryValue,
            String statusCode,
            String drilldownReferenceJson,
            OffsetDateTime capturedAt) {
        return DashboardMetricSnapshot.builder()
                .id(UUID.randomUUID())
                .metricDefinition(metricDefinition)
                .branch(branch)
                .snapshotDate(snapshotDate)
                .primaryValue(primaryValue)
                .secondaryValue(secondaryValue)
                .statusCode(statusCode)
                .drilldownReferenceJson(drilldownReferenceJson)
                .capturedAt(capturedAt)
                .build();
    }

    public void recalculate(
            Branch branch,
            int primaryValue,
            int secondaryValue,
            String statusCode,
            String drilldownReferenceJson,
            OffsetDateTime capturedAt) {
        assignBranch(branch);
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
        this.statusCode = statusCode;
        this.drilldownReferenceJson = drilldownReferenceJson;
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        validateState();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignMetricDefinition(DashboardMetricDefinition metricDefinition) {
        this.metricDefinition = Objects.requireNonNull(metricDefinition, "metricDefinition must not be null");
        assignAgency(metricDefinition.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the metric snapshot");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        drilldownReferenceJson = normalizeRequired(drilldownReferenceJson, "drilldownReferenceJson");
        statusCode = normalizeOptional(statusCode);
        assignMetricDefinition(metricDefinition);
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (primaryValue < 0 || secondaryValue < 0) {
            throw new IllegalArgumentException("metric snapshot values must be greater than or equal to 0");
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
