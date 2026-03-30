package com.homehealthcare.analytics.domain;

import com.homehealthcare.analytics.foundation.AnalyticsBacklogPosture;
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
@Table(name = "backlog_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BacklogSummary extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "documentation_aging_count", nullable = false)
    private int documentationAgingCount;

    @Column(name = "aging_bucket_json", nullable = false, length = 2000)
    private String agingBucketJson;

    @Column(name = "pending_review_count", nullable = false)
    private int pendingReviewCount;

    @Column(name = "returned_for_fix_count", nullable = false)
    private int returnedForFixCount;

    @Column(name = "overdue_review_count", nullable = false)
    private int overdueReviewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "posture", nullable = false, length = 24)
    private AnalyticsBacklogPosture posture;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private BacklogSummary(
            UUID id,
            Branch branch,
            LocalDate snapshotDate,
            int documentationAgingCount,
            String agingBucketJson,
            int pendingReviewCount,
            int returnedForFixCount,
            int overdueReviewCount,
            AnalyticsBacklogPosture posture,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.documentationAgingCount = documentationAgingCount;
        this.agingBucketJson = agingBucketJson;
        this.pendingReviewCount = pendingReviewCount;
        this.returnedForFixCount = returnedForFixCount;
        this.overdueReviewCount = overdueReviewCount;
        this.posture = Objects.requireNonNull(posture, "posture must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static BacklogSummary create(
            Branch branch,
            LocalDate snapshotDate,
            int documentationAgingCount,
            String agingBucketJson,
            int pendingReviewCount,
            int returnedForFixCount,
            int overdueReviewCount,
            AnalyticsBacklogPosture posture,
            OffsetDateTime evaluatedAt) {
        return BacklogSummary.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .snapshotDate(snapshotDate)
                .documentationAgingCount(documentationAgingCount)
                .agingBucketJson(agingBucketJson)
                .pendingReviewCount(pendingReviewCount)
                .returnedForFixCount(returnedForFixCount)
                .overdueReviewCount(overdueReviewCount)
                .posture(posture)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            int documentationAgingCount,
            String agingBucketJson,
            int pendingReviewCount,
            int returnedForFixCount,
            int overdueReviewCount,
            AnalyticsBacklogPosture posture,
            OffsetDateTime evaluatedAt) {
        this.documentationAgingCount = documentationAgingCount;
        this.agingBucketJson = agingBucketJson;
        this.pendingReviewCount = pendingReviewCount;
        this.returnedForFixCount = returnedForFixCount;
        this.overdueReviewCount = overdueReviewCount;
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
        agingBucketJson = normalizeRequired(agingBucketJson, "agingBucketJson");
        posture = Objects.requireNonNull(posture, "posture must not be null");
        evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (documentationAgingCount < 0 || pendingReviewCount < 0 || returnedForFixCount < 0 || overdueReviewCount < 0) {
            throw new IllegalArgumentException("backlog values must be greater than or equal to 0");
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
