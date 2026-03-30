package com.homehealthcare.review.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
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
@Table(name = "return_for_fix_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReturnForFixEvent extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_source_type", nullable = false, length = 48)
    private ReviewSourceType targetSourceType;

    @Column(name = "target_source_record_id", nullable = false)
    private UUID targetSourceRecordId;

    @Column(name = "return_reason", nullable = false, length = 1000)
    private String returnReason;

    @Column(name = "required_corrections", length = 2000)
    private String requiredCorrections;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "returned_by_membership_id", nullable = false)
    private AgencyMembership returnedByMembership;

    @Column(name = "returned_at", nullable = false)
    private OffsetDateTime returnedAt;

    @Column(name = "resubmitted_at")
    private OffsetDateTime resubmittedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private ReturnForFixEvent(
            UUID id,
            ReviewWorkItem workItem,
            ReviewSourceType targetSourceType,
            UUID targetSourceRecordId,
            String returnReason,
            String requiredCorrections,
            AgencyMembership returnedByMembership,
            OffsetDateTime returnedAt,
            OffsetDateTime resubmittedAt,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignWorkItem(workItem);
        this.targetSourceType = Objects.requireNonNull(targetSourceType, "targetSourceType must not be null");
        this.targetSourceRecordId = Objects.requireNonNull(targetSourceRecordId, "targetSourceRecordId must not be null");
        this.returnReason = returnReason;
        this.requiredCorrections = requiredCorrections;
        assignReturnedByMembership(returnedByMembership);
        this.returnedAt = Objects.requireNonNull(returnedAt, "returnedAt must not be null");
        this.resubmittedAt = resubmittedAt;
        this.resolvedAt = resolvedAt;
    }

    public static ReturnForFixEvent create(
            ReviewWorkItem workItem,
            ReviewSourceType targetSourceType,
            UUID targetSourceRecordId,
            String returnReason,
            String requiredCorrections,
            AgencyMembership returnedByMembership,
            OffsetDateTime returnedAt) {
        return ReturnForFixEvent.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .targetSourceType(targetSourceType)
                .targetSourceRecordId(targetSourceRecordId)
                .returnReason(returnReason)
                .requiredCorrections(requiredCorrections)
                .returnedByMembership(returnedByMembership)
                .returnedAt(returnedAt)
                .build();
    }

    public void markResubmitted(OffsetDateTime resubmittedAt) {
        this.resubmittedAt = Objects.requireNonNull(resubmittedAt, "resubmittedAt must not be null");
    }

    public void markResolved(OffsetDateTime resolvedAt) {
        this.resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    private void assignReturnedByMembership(AgencyMembership returnedByMembership) {
        this.returnedByMembership = Objects.requireNonNull(returnedByMembership, "returnedByMembership must not be null");
        if (!Objects.equals(returnedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("returnedByMembership must belong to the same agency as the return-for-fix event");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        returnReason = Objects.requireNonNull(returnReason, "returnReason must not be null").trim();
        requiredCorrections = normalizeOptional(requiredCorrections);
        if (returnReason.isBlank()) {
            throw new IllegalArgumentException("returnReason must not be blank");
        }
        if (resubmittedAt != null && resubmittedAt.isBefore(returnedAt)) {
            throw new IllegalArgumentException("resubmittedAt must not be before returnedAt");
        }
        if (resolvedAt != null && resolvedAt.isBefore(returnedAt)) {
            throw new IllegalArgumentException("resolvedAt must not be before returnedAt");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
