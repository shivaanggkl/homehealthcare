package com.homehealthcare.analytics.domain;

import com.homehealthcare.analytics.foundation.AnalyticsBranchPerformancePosture;
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
@Table(name = "branch_performance_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchPerformanceSummary extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "posture", nullable = false, length = 24)
    private AnalyticsBranchPerformancePosture posture;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private BranchPerformanceSummary(
            UUID id,
            Branch branch,
            LocalDate snapshotDate,
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            AnalyticsBranchPerformancePosture posture,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.todaysVisitCount = todaysVisitCount;
        this.unfilledVisitCount = unfilledVisitCount;
        this.lateStartCount = lateStartCount;
        this.missedVisitCount = missedVisitCount;
        this.documentationAgingCount = documentationAgingCount;
        this.qaBacklogCount = qaBacklogCount;
        this.posture = Objects.requireNonNull(posture, "posture must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static BranchPerformanceSummary create(
            Branch branch,
            LocalDate snapshotDate,
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            AnalyticsBranchPerformancePosture posture,
            OffsetDateTime evaluatedAt) {
        return BranchPerformanceSummary.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .snapshotDate(snapshotDate)
                .todaysVisitCount(todaysVisitCount)
                .unfilledVisitCount(unfilledVisitCount)
                .lateStartCount(lateStartCount)
                .missedVisitCount(missedVisitCount)
                .documentationAgingCount(documentationAgingCount)
                .qaBacklogCount(qaBacklogCount)
                .posture(posture)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            int todaysVisitCount,
            int unfilledVisitCount,
            int lateStartCount,
            int missedVisitCount,
            int documentationAgingCount,
            int qaBacklogCount,
            AnalyticsBranchPerformancePosture posture,
            OffsetDateTime evaluatedAt) {
        this.todaysVisitCount = todaysVisitCount;
        this.unfilledVisitCount = unfilledVisitCount;
        this.lateStartCount = lateStartCount;
        this.missedVisitCount = missedVisitCount;
        this.documentationAgingCount = documentationAgingCount;
        this.qaBacklogCount = qaBacklogCount;
        this.posture = Objects.requireNonNull(posture, "posture must not be null");
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
        posture = Objects.requireNonNull(posture, "posture must not be null");
        evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (todaysVisitCount < 0
                || unfilledVisitCount < 0
                || lateStartCount < 0
                || missedVisitCount < 0
                || documentationAgingCount < 0
                || qaBacklogCount < 0) {
            throw new IllegalArgumentException("branch performance counts must be greater than or equal to 0");
        }
    }
}
