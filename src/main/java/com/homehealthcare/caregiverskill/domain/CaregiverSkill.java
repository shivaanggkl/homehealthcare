package com.homehealthcare.caregiverskill.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.configuration.foundation.AgencyConfigurationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "caregiver_skills")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverSkill extends AgencyConfigurationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder
    private CaregiverSkill(UUID id, Agency agency, String name, String code, String description) {
        this.id = id;
        assignAgency(agency);
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public static CaregiverSkill create(Agency agency, String name, String code, String description) {
        CaregiverSkill skill = CaregiverSkill.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .name(name)
                .code(code)
                .description(description)
                .build();
        skill.activate();
        return skill;
    }

    public void updateDetails(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = Objects.requireNonNull(name, "name must not be null").trim();
        code = Objects.requireNonNull(code, "code must not be null").trim().toUpperCase(Locale.ROOT);
        description = description == null || description.trim().isBlank() ? null : description.trim();
    }
}
