package com.homehealthcare.review.domain;

import com.homehealthcare.review.foundation.ReviewFindingKind;
import com.homehealthcare.review.foundation.ReviewFindingSeverity;
import com.homehealthcare.review.foundation.ReviewFindingStatus;
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
@Table(name = "review_findings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewFinding extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completeness_check_result_id")
    private CompletenessCheckResult completenessCheckResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_kind", nullable = false, length = 32)
    private ReviewFindingKind findingKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluated_source_type", nullable = false, length = 48)
    private ReviewSourceType evaluatedSourceType;

    @Column(name = "evaluated_source_id", nullable = false)
    private UUID evaluatedSourceId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 24)
    private ReviewFindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_status", nullable = false, length = 24)
    private ReviewFindingStatus findingStatus;

    @Column(name = "field_path", length = 255)
    private String fieldPath;

    @Column(name = "logical_section", length = 120)
    private String logicalSection;

    @Column(name = "explanation", nullable = false, length = 2000)
    private String explanation;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private ReviewFinding(
            UUID id,
            ReviewWorkItem workItem,
            CompletenessCheckResult completenessCheckResult,
            ReviewFindingKind findingKind,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            String ruleCode,
            ReviewFindingSeverity severity,
            ReviewFindingStatus findingStatus,
            String fieldPath,
            String logicalSection,
            String explanation,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignWorkItem(workItem);
        assignCompletenessCheckResult(completenessCheckResult);
        this.findingKind = Objects.requireNonNull(findingKind, "findingKind must not be null");
        this.evaluatedSourceType = Objects.requireNonNull(evaluatedSourceType, "evaluatedSourceType must not be null");
        this.evaluatedSourceId = Objects.requireNonNull(evaluatedSourceId, "evaluatedSourceId must not be null");
        this.ruleCode = ruleCode;
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.findingStatus = Objects.requireNonNull(findingStatus, "findingStatus must not be null");
        this.fieldPath = fieldPath;
        this.logicalSection = logicalSection;
        this.explanation = explanation;
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public static ReviewFinding create(
            ReviewWorkItem workItem,
            CompletenessCheckResult completenessCheckResult,
            ReviewFindingKind findingKind,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            String ruleCode,
            ReviewFindingSeverity severity,
            ReviewFindingStatus findingStatus,
            String fieldPath,
            String logicalSection,
            String explanation,
            OffsetDateTime evaluatedAt) {
        return ReviewFinding.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .completenessCheckResult(completenessCheckResult)
                .findingKind(findingKind)
                .evaluatedSourceType(evaluatedSourceType)
                .evaluatedSourceId(evaluatedSourceId)
                .ruleCode(ruleCode)
                .severity(severity)
                .findingStatus(findingStatus)
                .fieldPath(fieldPath)
                .logicalSection(logicalSection)
                .explanation(explanation)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    private void assignCompletenessCheckResult(CompletenessCheckResult completenessCheckResult) {
        if (completenessCheckResult != null && !Objects.equals(completenessCheckResult.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("completenessCheckResult must belong to the same agency as the review finding");
        }
        this.completenessCheckResult = completenessCheckResult;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        ruleCode = Objects.requireNonNull(ruleCode, "ruleCode must not be null").trim();
        explanation = Objects.requireNonNull(explanation, "explanation must not be null").trim();
        fieldPath = normalizeOptional(fieldPath);
        logicalSection = normalizeOptional(logicalSection);
        if (ruleCode.isBlank() || explanation.isBlank()) {
            throw new IllegalArgumentException("review finding ruleCode and explanation must not be blank");
        }
        assignCompletenessCheckResult(completenessCheckResult);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
