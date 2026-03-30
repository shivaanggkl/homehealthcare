package com.homehealthcare.review.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
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
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "review_assignments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewAssignment extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_membership_id", nullable = false)
    private AgencyMembership reviewerMembership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_membership_id", nullable = false)
    private AgencyMembership assignedByMembership;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "assignment_note", length = 1000)
    private String assignmentNote;

    @Builder
    private ReviewAssignment(
            UUID id,
            ReviewWorkItem workItem,
            AgencyMembership reviewerMembership,
            AgencyMembership assignedByMembership,
            OffsetDateTime assignedAt,
            OffsetDateTime releasedAt,
            String assignmentNote) {
        this.id = id;
        assignWorkItem(workItem);
        assignReviewerMembership(reviewerMembership);
        assignAssignedByMembership(assignedByMembership);
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.releasedAt = releasedAt;
        this.assignmentNote = assignmentNote;
    }

    public static ReviewAssignment assign(
            ReviewWorkItem workItem,
            AgencyMembership reviewerMembership,
            AgencyMembership assignedByMembership,
            OffsetDateTime assignedAt,
            String assignmentNote) {
        return ReviewAssignment.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .reviewerMembership(reviewerMembership)
                .assignedByMembership(assignedByMembership)
                .assignedAt(assignedAt)
                .assignmentNote(assignmentNote)
                .build();
    }

    public void release(OffsetDateTime releasedAt) {
        this.releasedAt = Objects.requireNonNull(releasedAt, "releasedAt must not be null");
    }

    public boolean isActive() {
        return releasedAt == null;
    }

    public UUID getWorkItemId() {
        return workItem == null ? null : workItem.getId();
    }

    public UUID getReviewerMembershipId() {
        return reviewerMembership == null ? null : reviewerMembership.getId();
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    private void assignReviewerMembership(AgencyMembership reviewerMembership) {
        this.reviewerMembership = Objects.requireNonNull(reviewerMembership, "reviewerMembership must not be null");
        if (!Objects.equals(reviewerMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("reviewerMembership must belong to the same agency as the review assignment");
        }
    }

    private void assignAssignedByMembership(AgencyMembership assignedByMembership) {
        this.assignedByMembership = Objects.requireNonNull(assignedByMembership, "assignedByMembership must not be null");
        if (!Objects.equals(assignedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("assignedByMembership must belong to the same agency as the review assignment");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignmentNote = normalizeOptional(assignmentNote);
        if (releasedAt != null && releasedAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("releasedAt must not be before assignedAt");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
