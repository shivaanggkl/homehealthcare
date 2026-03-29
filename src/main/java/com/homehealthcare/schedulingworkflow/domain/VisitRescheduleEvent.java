package com.homehealthcare.schedulingworkflow.domain;

import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
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
@Table(name = "visit_reschedule_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VisitRescheduleEvent extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_caregiver_profile_id")
    private CaregiverProfile previousCaregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_caregiver_profile_id")
    private CaregiverProfile newCaregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rescheduled_by_membership_id", nullable = false)
    private AgencyMembership rescheduledByMembership;

    @Column(name = "previous_planned_start_at", nullable = false)
    private OffsetDateTime previousPlannedStartAt;

    @Column(name = "previous_planned_end_at", nullable = false)
    private OffsetDateTime previousPlannedEndAt;

    @Column(name = "new_planned_start_at", nullable = false)
    private OffsetDateTime newPlannedStartAt;

    @Column(name = "new_planned_end_at", nullable = false)
    private OffsetDateTime newPlannedEndAt;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "rescheduled_at", nullable = false)
    private OffsetDateTime rescheduledAt;

    @Builder
    private VisitRescheduleEvent(
            UUID id,
            VisitOccurrence visitOccurrence,
            CaregiverProfile previousCaregiverProfile,
            CaregiverProfile newCaregiverProfile,
            AgencyMembership rescheduledByMembership,
            OffsetDateTime previousPlannedStartAt,
            OffsetDateTime previousPlannedEndAt,
            OffsetDateTime newPlannedStartAt,
            OffsetDateTime newPlannedEndAt,
            String reason,
            OffsetDateTime rescheduledAt) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignCaregiver(previousCaregiverProfile);
        assignCaregiver(newCaregiverProfile);
        assignRescheduledByMembership(rescheduledByMembership);
        this.previousPlannedStartAt = Objects.requireNonNull(previousPlannedStartAt, "previousPlannedStartAt must not be null");
        this.previousPlannedEndAt = Objects.requireNonNull(previousPlannedEndAt, "previousPlannedEndAt must not be null");
        this.newPlannedStartAt = Objects.requireNonNull(newPlannedStartAt, "newPlannedStartAt must not be null");
        this.newPlannedEndAt = Objects.requireNonNull(newPlannedEndAt, "newPlannedEndAt must not be null");
        this.reason = reason;
        this.rescheduledAt = Objects.requireNonNull(rescheduledAt, "rescheduledAt must not be null");
        this.previousCaregiverProfile = previousCaregiverProfile;
        this.newCaregiverProfile = newCaregiverProfile;
        validateState();
    }

    public static VisitRescheduleEvent create(
            VisitOccurrence visitOccurrence,
            CaregiverProfile previousCaregiverProfile,
            CaregiverProfile newCaregiverProfile,
            AgencyMembership rescheduledByMembership,
            OffsetDateTime previousPlannedStartAt,
            OffsetDateTime previousPlannedEndAt,
            OffsetDateTime newPlannedStartAt,
            OffsetDateTime newPlannedEndAt,
            String reason) {
        return VisitRescheduleEvent.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .previousCaregiverProfile(previousCaregiverProfile)
                .newCaregiverProfile(newCaregiverProfile)
                .rescheduledByMembership(rescheduledByMembership)
                .previousPlannedStartAt(previousPlannedStartAt)
                .previousPlannedEndAt(previousPlannedEndAt)
                .newPlannedStartAt(newPlannedStartAt)
                .newPlannedEndAt(newPlannedEndAt)
                .reason(reason)
                .rescheduledAt(OffsetDateTime.now())
                .build();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignCaregiver(CaregiverProfile caregiverProfile) {
        if (caregiverProfile != null && !Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the reschedule event");
        }
    }

    private void assignRescheduledByMembership(AgencyMembership rescheduledByMembership) {
        this.rescheduledByMembership = Objects.requireNonNull(rescheduledByMembership, "rescheduledByMembership must not be null");
        if (!Objects.equals(rescheduledByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("rescheduledByMembership must belong to the same agency as the reschedule event");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        reason = normalizeOptional(reason);
        assignCaregiver(previousCaregiverProfile);
        assignCaregiver(newCaregiverProfile);
        assignRescheduledByMembership(rescheduledByMembership);
        validateState();
    }

    private void validateState() {
        if (!previousPlannedEndAt.isAfter(previousPlannedStartAt)) {
            throw new IllegalArgumentException("previous planned window must end after it starts");
        }
        if (!newPlannedEndAt.isAfter(newPlannedStartAt)) {
            throw new IllegalArgumentException("new planned window must end after it starts");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
