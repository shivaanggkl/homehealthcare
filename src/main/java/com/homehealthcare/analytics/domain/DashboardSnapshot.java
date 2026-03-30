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
@Table(name = "dashboard_snapshots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardSnapshot extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "todays_visit_count", nullable = false)
    private int todaysVisitCount;

    @Column(name = "unfilled_visit_count", nullable = false)
    private int unfilledVisitCount;

    @Column(name = "late_start_count", nullable = false)
    private int lateStartCount;

    @Column(name = "missed_visit_count", nullable = false)
    private int missedVisitCount;

    @Column(name = "documentation_aging_count", nullable = false)
    private int documentationAgingCount;

    @Column(name = "qa_backlog_count", nullable = false)
    private int qaBacklogCount;

    @Column(name = "caregiver_utilization_count", nullable = false)
    private int caregiverUtilizationCount;

    @Column(name = "revenue_blocked_count", nullable = false)
    private int revenueBlockedCount;

    @Column(name = "compliance_exception_count", nullable = false)
    private int complianceExceptionCount;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Builder
    private DashboardSnapshot(
            UUID id,
            Branch branch,
            LocalDate snapshotDate,
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            int caregiverUtilizationCount,
            int revenueBlockedCount,
            int complianceExceptionCount,
            OffsetDateTime generatedAt) {
        this.id = id;
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.todaysVisitCount = todaysVisitCount;
        this.unfilledVisitCount = unfilledVisitCount;
        this.lateStartCount = lateStartCount;
        this.missedVisitCount = missedVisitCount;
        this.documentationAgingCount = documentationAgingCount;
        this.qaBacklogCount = qaBacklogCount;
        this.caregiverUtilizationCount = caregiverUtilizationCount;
        this.revenueBlockedCount = revenueBlockedCount;
        this.complianceExceptionCount = complianceExceptionCount;
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        validateState();
    }

    public static DashboardSnapshot create(
            com.homehealthcare.agency.domain.Agency agency,
            Branch branch,
            LocalDate snapshotDate,
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            int caregiverUtilizationCount,
            int revenueBlockedCount,
            int complianceExceptionCount,
            OffsetDateTime generatedAt) {
        DashboardSnapshot snapshot = DashboardSnapshot.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .snapshotDate(snapshotDate)
                .todaysVisitCount(todaysVisitCount)
                .unfilledVisitCount(unfilledVisitCount)
                .lateStartCount(lateStartCount)
                .missedVisitCount(missedVisitCount)
                .documentationAgingCount(documentationAgingCount)
                .qaBacklogCount(qaBacklogCount)
                .caregiverUtilizationCount(caregiverUtilizationCount)
                .revenueBlockedCount(revenueBlockedCount)
                .complianceExceptionCount(complianceExceptionCount)
                .generatedAt(generatedAt)
                .build();
        snapshot.assignAgency(agency);
        return snapshot;
    }

    public void regenerate(
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            int caregiverUtilizationCount,
            int revenueBlockedCount,
            int complianceExceptionCount,
            OffsetDateTime generatedAt) {
        this.todaysVisitCount = todaysVisitCount;
        this.unfilledVisitCount = unfilledVisitCount;
        this.lateStartCount = lateStartCount;
        this.missedVisitCount = missedVisitCount;
        this.documentationAgingCount = documentationAgingCount;
        this.qaBacklogCount = qaBacklogCount;
        this.caregiverUtilizationCount = caregiverUtilizationCount;
        this.revenueBlockedCount = revenueBlockedCount;
        this.complianceExceptionCount = complianceExceptionCount;
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        validateState();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null && getAgencyId() != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the dashboard snapshot");
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
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (todaysVisitCount < 0
                || unfilledVisitCount < 0
                || lateStartCount < 0
                || missedVisitCount < 0
                || documentationAgingCount < 0
                || qaBacklogCount < 0
                || caregiverUtilizationCount < 0
                || revenueBlockedCount < 0
                || complianceExceptionCount < 0) {
            throw new IllegalArgumentException("dashboard snapshot values must be greater than or equal to 0");
        }
    }
}
