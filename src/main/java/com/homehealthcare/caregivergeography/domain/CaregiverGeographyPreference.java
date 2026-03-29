package com.homehealthcare.caregivergeography.domain;

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
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_geography_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverGeographyPreference extends AgencyScopedEntity {

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
    @Column(name = "preference_type", nullable = false, length = 32)
    private CaregiverGeographyPreferenceType preferenceType;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 80)
    private String state;

    @Column(name = "anchor_latitude", precision = 10, scale = 6)
    private BigDecimal anchorLatitude;

    @Column(name = "anchor_longitude", precision = 10, scale = 6)
    private BigDecimal anchorLongitude;

    @Column(name = "radius_miles", precision = 8, scale = 2)
    private BigDecimal radiusMiles;

    @Column(name = "priority_rank")
    private Integer priorityRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverGeographyPreference(
            UUID id,
            CaregiverProfile caregiverProfile,
            Branch branch,
            CaregiverGeographyPreferenceType preferenceType,
            String postalCode,
            String city,
            String state,
            BigDecimal anchorLatitude,
            BigDecimal anchorLongitude,
            BigDecimal radiusMiles,
            Integer priorityRank,
            WorkforceLifecycleStatus status,
            String notes) {
        this.id = id;
        assignProfile(caregiverProfile);
        assignBranch(branch);
        this.preferenceType = Objects.requireNonNull(preferenceType, "preferenceType must not be null");
        this.postalCode = postalCode;
        this.city = city;
        this.state = state;
        this.anchorLatitude = anchorLatitude;
        this.anchorLongitude = anchorLongitude;
        this.radiusMiles = radiusMiles;
        this.priorityRank = priorityRank;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
        validateShape();
    }

    public static CaregiverGeographyPreference create(
            CaregiverProfile caregiverProfile,
            Branch branch,
            CaregiverGeographyPreferenceType preferenceType,
            String postalCode,
            String city,
            String state,
            BigDecimal anchorLatitude,
            BigDecimal anchorLongitude,
            BigDecimal radiusMiles,
            Integer priorityRank,
            String notes) {
        return CaregiverGeographyPreference.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .branch(branch)
                .preferenceType(preferenceType)
                .postalCode(postalCode)
                .city(city)
                .state(state)
                .anchorLatitude(anchorLatitude)
                .anchorLongitude(anchorLongitude)
                .radiusMiles(radiusMiles)
                .priorityRank(priorityRank)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public void updateDetails(
            Branch branch,
            CaregiverGeographyPreferenceType preferenceType,
            String postalCode,
            String city,
            String state,
            BigDecimal anchorLatitude,
            BigDecimal anchorLongitude,
            BigDecimal radiusMiles,
            Integer priorityRank,
            String notes) {
        assignBranch(branch);
        this.preferenceType = Objects.requireNonNull(preferenceType, "preferenceType must not be null");
        this.postalCode = postalCode;
        this.city = city;
        this.state = state;
        this.anchorLatitude = anchorLatitude;
        this.anchorLongitude = anchorLongitude;
        this.radiusMiles = radiusMiles;
        this.priorityRank = priorityRank;
        this.notes = notes;
        validateShape();
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
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
        assignBranch(branch);
        postalCode = normalizeOptional(postalCode);
        city = normalizeOptional(city);
        state = normalizeOptional(state);
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
        validateShape();
    }

    private void validateShape() {
        switch (preferenceType) {
            case BRANCH -> {
                if (branch == null) {
                    throw new IllegalArgumentException("branch preference requires branch");
                }
            }
            case POSTAL_CODE -> {
                if (postalCode == null || postalCode.isBlank()) {
                    throw new IllegalArgumentException("postal code preference requires postalCode");
                }
            }
            case CITY_STATE -> {
                if (city == null || city.isBlank() || state == null || state.isBlank()) {
                    throw new IllegalArgumentException("city/state preference requires city and state");
                }
            }
            case RADIUS -> {
                if (anchorLatitude == null || anchorLongitude == null || radiusMiles == null) {
                    throw new IllegalArgumentException("radius preference requires anchor coordinates and radius");
                }
            }
        }
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
