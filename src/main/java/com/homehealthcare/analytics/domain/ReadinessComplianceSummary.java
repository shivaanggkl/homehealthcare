package com.homehealthcare.analytics.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "readiness_compliance_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadinessComplianceSummary extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "revenue_ready_count", nullable = false)
    private int revenueReadyCount;

    @Column(name = "revenue_warning_count", nullable = false)
    private int revenueWarningCount;

    @Column(name = "revenue_blocked_count", nullable = false)
    private int revenueBlockedCount;

    @Column(name = "compliance_ready_count", nullable = false)
    private int complianceReadyCount;

    @Column(name = "compliance_warning_count", nullable = false)
    private int complianceWarningCount;

    @Column(name = "compliance_exception_count", nullable = false)
    private int complianceExceptionCount;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private ReadinessComplianceSummary(
            UUID id,
            Branch branch,
            LocalDate snapshotDate,
            int revenueReadyCount,
            int revenueWarningCount,
            int revenueBlockedCount,
            int complianceReadyCount,
            int complianceWarningCount,
            int complianceExceptionCount,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.revenueReadyCount = revenueReadyCount;
        this.revenueWarningCount = revenueWarningCount;
        this.revenueBlockedCount = revenueBlockedCount;
        this.complianceReadyCount = complianceReadyCount;
        this.complianceWarningCount = complianceWarningCount;
        this.complianceExceptionCount = complianceExceptionCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static ReadinessComplianceSummary create(
            Branch branch,
            LocalDate snapshotDate,
            int revenueReadyCount,
            int revenueWarningCount,
            int revenueBlockedCount,
            int complianceReadyCount,
            int complianceWarningCount,
            int complianceExceptionCount,
            OffsetDateTime evaluatedAt) {
        return ReadinessComplianceSummary.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .snapshotDate(snapshotDate)
                .revenueReadyCount(revenueReadyCount)
                .revenueWarningCount(revenueWarningCount)
                .revenueBlockedCount(revenueBlockedCount)
                .complianceReadyCount(complianceReadyCount)
                .complianceWarningCount(complianceWarningCount)
                .complianceExceptionCount(complianceExceptionCount)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            int revenueReadyCount,
            int revenueWarningCount,
            int revenueBlockedCount,
            int complianceReadyCount,
            int complianceWarningCount,
            int complianceExceptionCount,
            OffsetDateTime evaluatedAt) {
        this.revenueReadyCount = revenueReadyCount;
        this.revenueWarningCount = revenueWarningCount;
        this.revenueBlockedCount = revenueBlockedCount;
        this.complianceReadyCount = complianceReadyCount;
        this.complianceWarningCount = complianceWarningCount;
        this.complianceExceptionCount = complianceExceptionCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public UUID getBranchId() {
        return branch.getId();
    }

    private void assignBranch(Branch branch) {
        this.branch = Objects.requireNonNull(branch, "branch must not be null");
        assignAgency(branch.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (revenueReadyCount < 0
                || revenueWarningCount < 0
                || revenueBlockedCount < 0
                || complianceReadyCount < 0
                || complianceWarningCount < 0
                || complianceExceptionCount < 0) {
            throw new IllegalArgumentException("readiness/compliance values must be greater than or equal to 0");
        }
    }
}
