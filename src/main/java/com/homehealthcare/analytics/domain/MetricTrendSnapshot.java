package com.homehealthcare.analytics.domain;

import com.homehealthcare.analytics.foundation.AnalyticsDashboardMetricType;
import com.homehealthcare.analytics.foundation.AnalyticsTrendDirection;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
@Table(name = "metric_trend_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetricTrendSnapshot extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 48)
    private AnalyticsDashboardMetricType metricType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "current_value", nullable = false)
    private int currentValue;

    @Column(name = "previous_value", nullable = false)
    private int previousValue;

    @Column(name = "delta_value", nullable = false)
    private int deltaValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private AnalyticsTrendDirection direction;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    @Builder
    private MetricTrendSnapshot(
            UUID id,
            AnalyticsDashboardMetricType metricType,
            Branch branch,
            LocalDate snapshotDate,
            int currentValue,
            int previousValue,
            int deltaValue,
            AnalyticsTrendDirection direction,
            OffsetDateTime calculatedAt) {
        this.id = id;
        this.metricType = Objects.requireNonNull(metricType, "metricType must not be null");
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.currentValue = currentValue;
        this.previousValue = previousValue;
        this.deltaValue = deltaValue;
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
    }

    public static MetricTrendSnapshot create(
            DashboardMetricDefinition metricDefinition,
            Branch branch,
            LocalDate snapshotDate,
            int currentValue,
            int previousValue,
            int deltaValue,
            AnalyticsTrendDirection direction,
            OffsetDateTime calculatedAt) {
        MetricTrendSnapshot snapshot = MetricTrendSnapshot.builder()
                .id(UUID.randomUUID())
                .metricType(metricDefinition.getMetricType())
                .branch(branch)
                .snapshotDate(snapshotDate)
                .currentValue(currentValue)
                .previousValue(previousValue)
                .deltaValue(deltaValue)
                .direction(direction)
                .calculatedAt(calculatedAt)
                .build();
        snapshot.assignAgency(metricDefinition.getAgency());
        return snapshot;
    }

    public void recalculate(
            int currentValue,
            int previousValue,
            int deltaValue,
            AnalyticsTrendDirection direction,
            OffsetDateTime calculatedAt) {
        this.currentValue = currentValue;
        this.previousValue = previousValue;
        this.deltaValue = deltaValue;
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null && getAgencyId() != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the trend snapshot");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        metricType = Objects.requireNonNull(metricType, "metricType must not be null");
        snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        direction = Objects.requireNonNull(direction, "direction must not be null");
        calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
        assignBranch(branch);
    }
}
