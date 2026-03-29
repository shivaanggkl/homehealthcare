package com.homehealthcare.caregiverlanguage.domain;

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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_languages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverLanguageProfile extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @Column(name = "language_code", nullable = false, length = 35)
    private String languageCode;

    @Column(name = "proficiency_level", length = 40)
    private String proficiencyLevel;

    @Column(name = "primary_language", nullable = false)
    private boolean primaryLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Builder
    private CaregiverLanguageProfile(
            UUID id,
            CaregiverProfile caregiverProfile,
            String languageCode,
            String proficiencyLevel,
            boolean primaryLanguage,
            WorkforceLifecycleStatus status) {
        this.id = id;
        assignProfile(caregiverProfile);
        this.languageCode = languageCode;
        this.proficiencyLevel = proficiencyLevel;
        this.primaryLanguage = primaryLanguage;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static CaregiverLanguageProfile create(
            CaregiverProfile caregiverProfile,
            String languageCode,
            String proficiencyLevel,
            boolean primaryLanguage) {
        return CaregiverLanguageProfile.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .languageCode(languageCode)
                .proficiencyLevel(proficiencyLevel)
                .primaryLanguage(primaryLanguage)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .build();
    }

    public void updateDetails(String languageCode, String proficiencyLevel, boolean primaryLanguage) {
        this.languageCode = languageCode;
        this.proficiencyLevel = proficiencyLevel;
        this.primaryLanguage = primaryLanguage;
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile == null ? null : caregiverProfile.getId();
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
        languageCode = normalizeLanguageTag(languageCode);
        proficiencyLevel = normalizeOptional(proficiencyLevel);
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String normalizeLanguageTag(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("languageCode must not be blank");
        }
        if (!normalized.matches("(?i)^[a-z]{2,3}(-[a-z]{2})?$")) {
            throw new IllegalArgumentException("Invalid caregiver language tag");
        }
        return Locale.forLanguageTag(normalized).toLanguageTag();
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
