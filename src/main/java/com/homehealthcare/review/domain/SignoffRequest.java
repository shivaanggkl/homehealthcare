package com.homehealthcare.review.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.review.foundation.SignoffRequestStatus;
import com.homehealthcare.security.branch.AgencyRole;
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
@Table(name = "signoff_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignoffRequest extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_item_id", nullable = false)
    private ReviewWorkItem workItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_from_membership_id")
    private AgencyMembership requestedFromMembership;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_from_role", length = 64)
    private AgencyRole requestedFromRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_membership_id", nullable = false)
    private AgencyMembership requestedByMembership;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private SignoffRequestStatus status;

    @Column(name = "signoff_note", length = 2000)
    private String signoffNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_membership_id")
    private AgencyMembership completedByMembership;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Builder
    private SignoffRequest(
            UUID id,
            ReviewWorkItem workItem,
            AgencyMembership requestedFromMembership,
            AgencyRole requestedFromRole,
            AgencyMembership requestedByMembership,
            OffsetDateTime requestedAt,
            SignoffRequestStatus status,
            String signoffNote,
            AgencyMembership completedByMembership,
            OffsetDateTime completedAt) {
        this.id = id;
        assignWorkItem(workItem);
        assignRequestedFromMembership(requestedFromMembership);
        this.requestedFromRole = requestedFromRole;
        assignRequestedByMembership(requestedByMembership);
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.signoffNote = signoffNote;
        assignCompletedByMembership(completedByMembership);
        this.completedAt = completedAt;
    }

    public static SignoffRequest create(
            ReviewWorkItem workItem,
            AgencyMembership requestedFromMembership,
            AgencyRole requestedFromRole,
            AgencyMembership requestedByMembership,
            OffsetDateTime requestedAt,
            String signoffNote) {
        return SignoffRequest.builder()
                .id(UUID.randomUUID())
                .workItem(workItem)
                .requestedFromMembership(requestedFromMembership)
                .requestedFromRole(requestedFromRole)
                .requestedByMembership(requestedByMembership)
                .requestedAt(requestedAt)
                .status(SignoffRequestStatus.PENDING)
                .signoffNote(signoffNote)
                .build();
    }

    public void complete(AgencyMembership completedByMembership, OffsetDateTime completedAt, String signoffNote) {
        assignCompletedByMembership(completedByMembership);
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = SignoffRequestStatus.COMPLETED;
        this.signoffNote = signoffNote;
    }

    public void decline(AgencyMembership completedByMembership, OffsetDateTime completedAt, String signoffNote) {
        assignCompletedByMembership(completedByMembership);
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = SignoffRequestStatus.DECLINED;
        this.signoffNote = signoffNote;
    }

    private void assignWorkItem(ReviewWorkItem workItem) {
        this.workItem = Objects.requireNonNull(workItem, "workItem must not be null");
        assignAgency(workItem.getAgency());
    }

    private void assignRequestedFromMembership(AgencyMembership requestedFromMembership) {
        if (requestedFromMembership != null && !Objects.equals(requestedFromMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("requestedFromMembership must belong to the same agency as the signoff request");
        }
        this.requestedFromMembership = requestedFromMembership;
    }

    private void assignRequestedByMembership(AgencyMembership requestedByMembership) {
        this.requestedByMembership = Objects.requireNonNull(requestedByMembership, "requestedByMembership must not be null");
        if (!Objects.equals(requestedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("requestedByMembership must belong to the same agency as the signoff request");
        }
    }

    private void assignCompletedByMembership(AgencyMembership completedByMembership) {
        if (completedByMembership != null && !Objects.equals(completedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("completedByMembership must belong to the same agency as the signoff request");
        }
        this.completedByMembership = completedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        signoffNote = normalizeOptional(signoffNote);
        if (requestedFromMembership == null && requestedFromRole == null) {
            throw new IllegalArgumentException("signoff request requires requestedFromMembership or requestedFromRole");
        }
        if (completedAt != null && completedAt.isBefore(requestedAt)) {
            throw new IllegalArgumentException("completedAt must not be before requestedAt");
        }
        assignRequestedFromMembership(requestedFromMembership);
        assignCompletedByMembership(completedByMembership);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
