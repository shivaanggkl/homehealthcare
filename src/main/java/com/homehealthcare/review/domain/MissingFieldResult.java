package com.homehealthcare.review.domain;

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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "missing_field_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissingFieldResult extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "completeness_check_result_id", nullable = false)
    private CompletenessCheckResult completenessCheckResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_finding_id", nullable = false)
    private ReviewFinding reviewFinding;

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

    @Column(name = "field_path", nullable = false, length = 255)
    private String fieldPath;

    @Column(name = "logical_section", length = 120)
    private String logicalSection;

    @Column(name = "explanation", nullable = false, length = 2000)
    private String explanation;

    @Builder
    private MissingFieldResult(
            UUID id,
            ReviewWorkItem workItem,
            CompletenessCheckResult completenessCheckResult,
            ReviewFinding reviewFinding,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            String ruleCode,
            ReviewFindingSeverity severity,
            ReviewFindingStatus findingStatus,
            String fieldPath,
            String logicalSection,
            String explanation) {
        this.id = id;
        assignWorkItem(workItem);
        assignCompletenessCheckResult(completenessCheckResult);
        assignReviewFinding(reviewFinding);
        this.evaluatedSourceType = Objects.requireNonNull(evaluatedSourceType, "evaluatedSourceType must not be null");
        this.evaluatedSourceId = Objects.requireNonNull(evaluatedSourceId, "evaluatedSourceId must not be null");
        this.ruleCode = ruleCode;
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.findingStatus = Objects.requireNonNull(findingStatus, "findingStatus must not be null");
        this.fieldPath = fieldPath;
        this.logicalSection = logicalSection;
        this.explanation = explanation;
    }

    public static MissingFieldResult create(
            ReviewWorkItem workItem,
            CompletenessCheckResult completenessCheckResult,
            ReviewFinding reviewFinding,
            ReviewSourceType evaluatedSourceType,
            UUID evaluatedSourceId,
            String ruleCode,
            ReviewFindingSeverity severity,
            ReviewFindingStatus findingStatus,
            String fieldPath,
            String logicalSection,
            String explanation) {
        return MissingFieldResult.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .completenessCheckResult(completenessCheckResult)
                .reviewFinding(reviewFinding)
                .evaluatedSourceType(evaluatedSourceType)
                .evaluatedSourceId(evaluatedSourceId)
                .ruleCode(ruleCode)
                .severity(severity)
                .findingStatus(findingStatus)
                .fieldPath(fieldPath)
                .logicalSection(logicalSection)
                .explanation(explanation)
                .build();
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    private void assignCompletenessCheckResult(CompletenessCheckResult completenessCheckResult) {
        this.completenessCheckResult = Objects.requireNonNull(completenessCheckResult, "completenessCheckResult must not be null");
        if (!Objects.equals(completenessCheckResult.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("completenessCheckResult must belong to the same agency as the missing field result");
        }
    }

    private void assignReviewFinding(ReviewFinding reviewFinding) {
        this.reviewFinding = Objects.requireNonNull(reviewFinding, "reviewFinding must not be null");
        if (!Objects.equals(reviewFinding.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("reviewFinding must belong to the same agency as the missing field result");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        ruleCode = Objects.requireNonNull(ruleCode, "ruleCode must not be null").trim();
        fieldPath = Objects.requireNonNull(fieldPath, "fieldPath must not be null").trim();
        explanation = Objects.requireNonNull(explanation, "explanation must not be null").trim();
        logicalSection = normalizeOptional(logicalSection);
        if (ruleCode.isBlank() || fieldPath.isBlank() || explanation.isBlank()) {
            throw new IllegalArgumentException("missing field results require non-blank ruleCode, fieldPath, and explanation");
        }
        assignCompletenessCheckResult(completenessCheckResult);
        assignReviewFinding(reviewFinding);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
