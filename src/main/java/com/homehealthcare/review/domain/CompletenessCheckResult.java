package com.homehealthcare.review.domain;

import com.homehealthcare.review.foundation.ReviewSourceType;
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
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "completeness_check_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompletenessCheckResult extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluated_source_type", nullable = false, length = 48)
    private ReviewSourceType evaluatedSourceType;

    @Column(name = "evaluated_source_id", nullable = false)
    private UUID evaluatedSourceId;

    @Column(name = "run_number", nullable = false)
    private int runNumber;

    @Column(name = "pass_count", nullable = false)
    private int passCount;

    @Column(name = "warning_count", nullable = false)
    private int warningCount;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private CompletenessCheckResult(
            UUID id,
            ReviewWorkItem workItem,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            int runNumber,
            int passCount,
            int warningCount,
            int failCount,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignWorkItem(workItem);
        this.evaluatedSourceType = Objects.requireNonNull(evaluatedSourceType, "evaluatedSourceType must not be null");
        this.evaluatedSourceId = Objects.requireNonNull(evaluatedSourceId, "evaluatedSourceId must not be null");
        this.runNumber = runNumber;
        this.passCount = passCount;
        this.warningCount = warningCount;
        this.failCount = failCount;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public static CompletenessCheckResult create(
            ReviewWorkItem workItem,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            int runNumber,
            int passCount,
            int warningCount,
            int failCount,
            OffsetDateTime evaluatedAt) {
        return CompletenessCheckResult.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .evaluatedSourceType(evaluatedSourceType)
                .evaluatedSourceId(evaluatedSourceId)
                .runNumber(runNumber)
                .passCount(passCount)
                .warningCount(warningCount)
                .failCount(failCount)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (runNumber <= 0) {
            throw new IllegalArgumentException("runNumber must be greater than 0");
        }
        if (passCount < 0 || warningCount < 0 || failCount < 0) {
            throw new IllegalArgumentException("finding counts must be greater than or equal to 0");
        }
    }
}
