package com.homehealthcare.caregiverskillprofile.domain;

import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_skill_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverSkillProfile extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private CaregiverSkill skill;

    @Column(name = "proficiency_level", length = 40)
    private String proficiencyLevel;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkforceLifecycleStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverSkillProfile(
            UUID id,
            CaregiverProfile caregiverProfile,
            CaregiverSkill skill,
            String proficiencyLevel,
            boolean verified,
            WorkforceLifecycleStatus status,
            String notes) {
        this.id = id;
        assignProfile(caregiverProfile);
        assignSkill(skill);
        this.proficiencyLevel = proficiencyLevel;
        this.verified = verified;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.notes = notes;
    }

    public static CaregiverSkillProfile create(
            CaregiverProfile caregiverProfile,
            CaregiverSkill skill,
            String proficiencyLevel,
            boolean verified,
            String notes) {
        return CaregiverSkillProfile.builder()
                .id(UUID.randomUUID())
                .caregiverProfile(caregiverProfile)
                .skill(skill)
                .proficiencyLevel(proficiencyLevel)
                .verified(verified)
                .status(WorkforceLifecycleStatus.ACTIVE)
                .notes(notes)
                .build();
    }

    public void updateDetails(CaregiverSkill skill, String proficiencyLevel, boolean verified, String notes) {
        assignSkill(skill);
        this.proficiencyLevel = proficiencyLevel;
        this.verified = verified;
        this.notes = notes;
    }

    public void deactivate() {
        this.status = WorkforceLifecycleStatus.INACTIVE;
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile == null ? null : caregiverProfile.getId();
    }

    public UUID getSkillId() {
        return skill == null ? null : skill.getId();
    }

    private void assignProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        assignAgency(caregiverProfile.getAgency());
    }

    private void assignSkill(CaregiverSkill skill) {
        this.skill = Objects.requireNonNull(skill, "skill must not be null");
        if (!Objects.equals(skill.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("skill must belong to the same agency as the caregiver profile");
        }
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
        assignSkill(skill);
        proficiencyLevel = normalizeOptional(proficiencyLevel);
        notes = normalizeOptional(notes);
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
