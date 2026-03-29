package com.homehealthcare.caregiveravailability.domain;

import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
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
@Table(name = "caregiver_unavailabilities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverUnavailability extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 32)
    private CaregiverUnavailabilityReasonType reasonType;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 32)
    private CaregiverUnavailabilityApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverUnavailability(
            UUID id,
            CaregiverProfile caregiverProfile,
            CaregiverUnavailabilityReasonType reasonType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            boolean allDay,
            CaregiverUnavailabilityApprovalStatus approvalStatus,
            WorkforceLifecycleStatus status,
            String notes) {
        this.id = id;
        assignProfile(caregiverProfile);
        this.reasonType = Objects.requireNonNull(reasonType, "reasonType must not be null");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
        this.allDay = allDay;
        this.approvalStatus = approvalStatus;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateWindow();
    }

    public static CaregiverUnavailability create(
            CaregiverProfile caregiverProfile,
            CaregiverUnavailabilityReasonType reasonType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            boolean allDay,
            CaregiverUnavailabilityApprovalStatus approvalStatus,
            String notes) {
        return CaregiverUnavailability.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .reasonType(reasonType)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .allDay(allDay)
                .approvalStatus(approvalStatus)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            CaregiverUnavailabilityReasonType reasonType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            boolean allDay,
            CaregiverUnavailabilityApprovalStatus approvalStatus,
            String notes) {
        this.reasonType = Objects.requireNonNull(reasonType, "reasonType must not be null");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
        this.allDay = allDay;
        this.approvalStatus = approvalStatus;
        this.notes = notes;
        validateWindow();
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
    }

    public boolean overlaps(CaregiverUnavailability other) {
        return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
    }

    private void assignProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        assignAgency(caregiverProfile.getAgency());
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (caregiverProfile == null) {
            throw new IllegalArgumentException("caregiverProfile must not be null");
        }
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        validateWindow();
    }

    private void validateWindow() {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
