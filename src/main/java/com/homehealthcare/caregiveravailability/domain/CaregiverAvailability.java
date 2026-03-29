package com.homehealthcare.caregiveravailability.domain;

import com.homehealthcare.branch.domain.Branch;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_availabilities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverAvailability extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_type", nullable = false, length = 32)
    private CaregiverAvailabilityType availabilityType;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverAvailability(
            UUID id,
            CaregiverProfile caregiverProfile,
            Branch branch,
            CaregiverAvailabilityType availabilityType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            WorkforceLifecycleStatus status,
            String notes) {
        this.id = id;
        assignProfile(caregiverProfile);
        assignBranch(branch);
        this.availabilityType = Objects.requireNonNull(availabilityType, "availabilityType must not be null");
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateShape();
    }

    public static CaregiverAvailability create(
            CaregiverProfile caregiverProfile,
            Branch branch,
            CaregiverAvailabilityType availabilityType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes) {
        return CaregiverAvailability.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .branch(branch)
                .availabilityType(availabilityType)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            Branch branch,
            CaregiverAvailabilityType availabilityType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes) {
        assignBranch(branch);
        this.availabilityType = Objects.requireNonNull(availabilityType, "availabilityType must not be null");
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.notes = notes;
        validateShape();
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile == null ? null : caregiverProfile.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public boolean overlaps(CaregiverAvailability other) {
        if (availabilityType != other.availabilityType) {
            return false;
        }
        if (availabilityType == CaregiverAvailabilityType.DATE_SPECIFIC) {
            return startsAt != null && endsAt != null && other.startsAt != null && other.endsAt != null
                    && startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt);
        }
        boolean sameDay = dayOfWeek == other.dayOfWeek;
        boolean dateOverlap = windowsOverlap(effectiveFrom, effectiveTo, other.effectiveFrom, other.effectiveTo);
        boolean timeOverlap = startTime != null && endTime != null && other.startTime != null && other.endTime != null
                && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
        return sameDay && dateOverlap && timeOverlap;
    }

    private void assignProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        assignAgency(caregiverProfile.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the caregiver profile");
        }
        this.branch = branch;
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
        assignBranch(branch);
        validateShape();
    }

    private void validateShape() {
        if (availabilityType == CaregiverAvailabilityType.DATE_SPECIFIC) {
            if (startsAt == null || endsAt == null) {
                throw new IllegalArgumentException("date-specific availability requires startsAt and endsAt");
            }
            if (!endsAt.isAfter(startsAt)) {
                throw new IllegalArgumentException("endsAt must be after startsAt");
            }
        } else {
            if (dayOfWeek == null || startTime == null || endTime == null) {
                throw new IllegalArgumentException("recurring availability requires dayOfWeek, startTime, and endTime");
            }
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("endTime must be after startTime");
            }
            if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                throw new IllegalArgumentException("effectiveTo must be on or after effectiveFrom");
            }
        }
    }

    private static boolean windowsOverlap(LocalDate startOne, LocalDate endOne, LocalDate startTwo, LocalDate endTwo) {
        LocalDate effectiveStartOne = startOne == null ? LocalDate.MIN : startOne;
        LocalDate effectiveEndOne = endOne == null ? LocalDate.MAX : endOne;
        LocalDate effectiveStartTwo = startTwo == null ? LocalDate.MIN : startTwo;
        LocalDate effectiveEndTwo = endTwo == null ? LocalDate.MAX : endTwo;
        return !effectiveEndOne.isBefore(effectiveStartTwo) && !effectiveEndTwo.isBefore(effectiveStartOne);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
