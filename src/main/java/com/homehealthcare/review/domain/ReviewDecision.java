package com.homehealthcare.review.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.review.foundation.ReviewDecisionType;
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
@Table(name = "review_decisions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewDecision extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 32)
    private ReviewDecisionType decisionType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decided_by_membership_id", nullable = false)
    private AgencyMembership decidedByMembership;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(name = "reviewer_notes", length = 2000)
    private String reviewerNotes;

    @Builder
    private ReviewDecision(
            UUID id,
            ReviewWorkItem workItem,
            ReviewDecisionType decisionType,
            AgencyMembership decidedByMembership,
            OffsetDateTime decidedAt,
            String reasonCode,
            String reviewerNotes) {
        this.id = id;
        assignWorkItem(workItem);
        this.decisionType = Objects.requireNonNull(decisionType, "decisionType must not be null");
        assignDecidedByMembership(decidedByMembership);
        this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        this.reasonCode = reasonCode;
        this.reviewerNotes = reviewerNotes;
    }

    public static ReviewDecision record(
            ReviewWorkItem workItem,
            ReviewDecisionType decisionType,
            AgencyMembership decidedByMembership,
            OffsetDateTime decidedAt,
            String reasonCode,
            String reviewerNotes) {
        return ReviewDecision.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .decisionType(decisionType)
                .decidedByMembership(decidedByMembership)
                .decidedAt(decidedAt)
                .reasonCode(reasonCode)
                .reviewerNotes(reviewerNotes)
                .build();
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    private void assignDecidedByMembership(AgencyMembership decidedByMembership) {
        this.decidedByMembership = Objects.requireNonNull(decidedByMembership, "decidedByMembership must not be null");
        if (!Objects.equals(decidedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("decidedByMembership must belong to the same agency as the review decision");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reasonCode = normalizeOptional(reasonCode);
        reviewerNotes = normalizeOptional(reviewerNotes);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
