package com.homehealthcare.review.domain;

import com.homehealthcare.review.foundation.ReviewExceptionType;
import com.homehealthcare.review.foundation.ReviewFindingSeverity;
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
@Table(name = "review_exceptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewExceptionRecord extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 48)
    private ReviewSourceType sourceType;

    @Column(name = "source_record_id", nullable = false)
    private UUID sourceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 48)
    private ReviewExceptionType exceptionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 24)
    private ReviewFindingSeverity severity;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Builder
    private ReviewExceptionRecord(
            UUID id,
            ReviewWorkItem workItem,
            ReviewSourceType sourceType,
            UUID sourceRecordId,
            ReviewExceptionType exceptionType,
            ReviewFindingSeverity severity,
            OffsetDateTime detectedAt,
            OffsetDateTime resolvedAt,
            String resolutionNote) {
        this.id = id;
        assignWorkItem(workItem);
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceRecordId = Objects.requireNonNull(sourceRecordId, "sourceRecordId must not be null");
        this.exceptionType = Objects.requireNonNull(exceptionType, "exceptionType must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt must not be null");
        this.resolvedAt = resolvedAt;
        this.resolutionNote = resolutionNote;
    }

    public static ReviewExceptionRecord create(
            ReviewWorkItem workItem,
            ReviewSourceType sourceType,
            UUID sourceRecordId,
            ReviewExceptionType exceptionType,
            ReviewFindingSeverity severity,
            OffsetDateTime detectedAt) {
        return ReviewExceptionRecord.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .sourceType(sourceType)
                .sourceRecordId(sourceRecordId)
                .exceptionType(exceptionType)
                .severity(severity)
                .detectedAt(detectedAt)
                .build();
    }

    public void resolve(OffsetDateTime resolvedAt, String resolutionNote) {
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
        this.resolutionNote = resolutionNote;
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
        resolutionNote = normalizeOptional(resolutionNote);
        if (resolvedAt != null && resolvedAt.isBefore(detectedAt)) {
            throw new IllegalArgumentException("resolvedAt must not be before detectedAt");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
