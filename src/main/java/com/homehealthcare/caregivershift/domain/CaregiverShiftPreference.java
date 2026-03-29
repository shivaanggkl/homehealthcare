package com.homehealthcare.caregivershift.domain;

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
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_shift_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverShiftPreference extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "preferred_start_time")
    private LocalTime preferredStartTime;

    @Column(name = "preferred_end_time")
    private LocalTime preferredEndTime;

    @Column(name = "preferred_shift_length_minutes")
    private Integer preferredShiftLengthMinutes;

    @Column(name = "preferred_visit_types", length = 500)
    private String preferredVisitTypes;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_strength", length = 32)
    private ShiftPreferenceStrength preferenceStrength;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverShiftPreference(
            UUID id,
            CaregiverProfile caregiverProfile,
            DayOfWeek dayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            Integer preferredShiftLengthMinutes,
            String preferredVisitTypes,
            ShiftPreferenceStrength preferenceStrength,
            WorkforceLifecycleStatus status,
            String notes) {
        this.id = id;
        assignProfile(caregiverProfile);
        this.dayOfWeek = dayOfWeek;
        this.preferredStartTime = preferredStartTime;
        this.preferredEndTime = preferredEndTime;
        this.preferredShiftLengthMinutes = preferredShiftLengthMinutes;
        this.preferredVisitTypes = preferredVisitTypes;
        this.preferenceStrength = preferenceStrength;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateWindow();
    }

    public static CaregiverShiftPreference create(
            CaregiverProfile caregiverProfile,
            DayOfWeek dayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            Integer preferredShiftLengthMinutes,
            String preferredVisitTypes,
            ShiftPreferenceStrength preferenceStrength,
            String notes) {
        return CaregiverShiftPreference.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .dayOfWeek(dayOfWeek)
                .preferredStartTime(preferredStartTime)
                .preferredEndTime(preferredEndTime)
                .preferredShiftLengthMinutes(preferredShiftLengthMinutes)
                .preferredVisitTypes(preferredVisitTypes)
                .preferenceStrength(preferenceStrength)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            DayOfWeek dayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            Integer preferredShiftLengthMinutes,
            String preferredVisitTypes,
            ShiftPreferenceStrength preferenceStrength,
            String notes) {
        this.dayOfWeek = dayOfWeek;
        this.preferredStartTime = preferredStartTime;
        this.preferredEndTime = preferredEndTime;
        this.preferredShiftLengthMinutes = preferredShiftLengthMinutes;
        this.preferredVisitTypes = preferredVisitTypes;
        this.preferenceStrength = preferenceStrength;
        this.notes = notes;
        validateWindow();
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
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
        preferredVisitTypes = normalizeOptional(preferredVisitTypes);
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        validateWindow();
    }

    private void validateWindow() {
        if (preferredStartTime != null && preferredEndTime != null && !preferredEndTime.isAfter(preferredStartTime)) {
            throw new IllegalArgumentException("preferredEndTime must be after preferredStartTime");
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
