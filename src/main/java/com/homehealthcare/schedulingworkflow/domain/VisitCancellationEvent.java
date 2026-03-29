package com.homehealthcare.schedulingworkflow.domain;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
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
@Table(name = "visit_cancellation_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitCancellationEvent extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancelled_by_membership_id", nullable = false)
    private AgencyMembership cancelledByMembership;

    @Column(name = "cancelled_at", nullable = false)
    private OffsetDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_party", length = 32)
    private VisitCancellationParty cancellationParty;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Builder
    private VisitCancellationEvent(
            UUID id,
            VisitOccurrence visitOccurrence,
            AgencyMembership cancelledByMembership,
            OffsetDateTime cancelledAt,
            VisitCancellationParty cancellationParty,
            String reason) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignCancelledByMembership(cancelledByMembership);
        this.cancelledAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        this.cancellationParty = cancellationParty;
        this.reason = reason;
    }

    public static VisitCancellationEvent create(
            VisitOccurrence visitOccurrence,
            AgencyMembership cancelledByMembership,
            VisitCancellationParty cancellationParty,
            String reason) {
        return VisitCancellationEvent.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .cancelledByMembership(cancelledByMembership)
                .cancelledAt(OffsetDateTime.now())
                .cancellationParty(cancellationParty)
                .reason(reason)
                .build();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignCancelledByMembership(AgencyMembership cancelledByMembership) {
        this.cancelledByMembership = Objects.requireNonNull(cancelledByMembership, "cancelledByMembership must not be null");
        if (!Objects.equals(cancelledByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("cancelledByMembership must belong to the same agency as the cancellation event");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reason = normalizeOptional(reason);
        assignCancelledByMembership(cancelledByMembership);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
