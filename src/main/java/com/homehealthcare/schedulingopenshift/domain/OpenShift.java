package com.homehealthcare.schedulingopenshift.domain;

import com.homehealthcare.branch.domain.Branch;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "open_shifts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenShift extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by_membership_id", nullable = false)
    private AgencyMembership openedByMembership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_membership_id")
    private AgencyMembership closedByMembership;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OpenShiftStatus status;

    @Column(name = "priority", length = 32)
    private String priority;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private OpenShift(
            UUID id,
            VisitOccurrence visitOccurrence,
            Branch branch,
            AgencyMembership openedByMembership,
            AgencyMembership closedByMembership,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt,
            OpenShiftStatus status,
            String priority,
            String notes) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignBranch(branch);
        assignOpenedByMembership(openedByMembership);
        assignClosedByMembership(closedByMembership);
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
        this.closedAt = closedAt;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.priority = priority;
        this.notes = notes;
    }

    public static OpenShift create(
            VisitOccurrence visitOccurrence,
            Branch branch,
            AgencyMembership openedByMembership,
            String priority,
            String notes) {
        return OpenShift.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .branch(branch)
                .openedByMembership(openedByMembership)
                .openedAt(OffsetDateTime.now())
                .status(OpenShiftStatus.OPEN)
                .priority(priority)
                .notes(notes)
                .build();
    }

    public void claimOrAssign(AgencyMembership closedByMembership) {
        this.status = OpenShiftStatus.CLAIMED_OR_ASSIGNED;
        this.closedByMembership = Objects.requireNonNull(closedByMembership, "closedByMembership must not be null");
        this.closedAt = OffsetDateTime.now();
    }

    public void cancel(AgencyMembership closedByMembership) {
        this.status = OpenShiftStatus.CANCELLED;
        this.closedByMembership = Objects.requireNonNull(closedByMembership, "closedByMembership must not be null");
        this.closedAt = OffsetDateTime.now();
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the open shift");
        }
        this.branch = branch;
    }

    private void assignOpenedByMembership(AgencyMembership openedByMembership) {
        this.openedByMembership = Objects.requireNonNull(openedByMembership, "openedByMembership must not be null");
        if (!Objects.equals(openedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("openedByMembership must belong to the same agency as the open shift");
        }
    }

    private void assignClosedByMembership(AgencyMembership closedByMembership) {
        if (closedByMembership != null && !Objects.equals(closedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("closedByMembership must belong to the same agency as the open shift");
        }
        this.closedByMembership = closedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        priority = normalizeOptional(priority);
        if (priority != null) {
            priority = priority.toUpperCase(Locale.ROOT);
        }
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignOpenedByMembership(openedByMembership);
        assignClosedByMembership(closedByMembership);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
