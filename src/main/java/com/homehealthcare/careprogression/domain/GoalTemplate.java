package com.homehealthcare.careprogression.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.careprogression.foundation.GoalTemplateLifecycleStatus;
import com.homehealthcare.serviceline.domain.ServiceLine;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
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
@Table(name = "goal_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalTemplate extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_line_id")
    private ServiceLine serviceLine;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "target_outcome_guidance", length = 2000)
    private String targetOutcomeGuidance;

    @Column(name = "default_intervention_scaffold", length = 2000)
    private String defaultInterventionScaffold;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GoalTemplateLifecycleStatus status;

    @Builder
    private GoalTemplate(
            UUID id,
            Agency agency,
            Branch branch,
            ServiceLine serviceLine,
            String name,
            String description,
            String targetOutcomeGuidance,
            String defaultInterventionScaffold,
            GoalTemplateLifecycleStatus status) {
        this.id = id;
        if (agency != null) {
            assignAgency(agency);
        }
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.name = name;
        this.description = description;
        this.targetOutcomeGuidance = targetOutcomeGuidance;
        this.defaultInterventionScaffold = defaultInterventionScaffold;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static GoalTemplate create(
            Agency agency,
            Branch branch,
            ServiceLine serviceLine,
            String name,
            String description,
            String targetOutcomeGuidance,
            String defaultInterventionScaffold,
            GoalTemplateLifecycleStatus status) {
        return GoalTemplate.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .branch(branch)
                .serviceLine(serviceLine)
                .name(name)
                .description(description)
                .targetOutcomeGuidance(targetOutcomeGuidance)
                .defaultInterventionScaffold(defaultInterventionScaffold)
                .status(status == null ? GoalTemplateLifecycleStatus.ACTIVE : status)
                .build();
    }

    public void updateDetails(
            Branch branch,
            ServiceLine serviceLine,
            String name,
            String description,
            String targetOutcomeGuidance,
            String defaultInterventionScaffold,
            GoalTemplateLifecycleStatus status) {
        assignBranch(branch);
        assignServiceLine(serviceLine);
        this.name = name;
        this.description = description;
        this.targetOutcomeGuidance = targetOutcomeGuidance;
        this.defaultInterventionScaffold = defaultInterventionScaffold;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void deactivate() {
        this.status = GoalTemplateLifecycleStatus.INACTIVE;
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getServiceLineId() {
        return serviceLine == null ? null : serviceLine.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null) {
            assignAgency(branch.getAgency());
        }
        if (branch != null && getAgencyId() != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the goal template");
        }
        this.branch = branch;
    }

    private void assignServiceLine(ServiceLine serviceLine) {
        if (serviceLine != null && getAgencyId() != null && !Objects.equals(serviceLine.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("serviceLine must belong to the same agency as the goal template");
        }
        this.serviceLine = serviceLine;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        name = required(name);
        description = optional(description);
        targetOutcomeGuidance = optional(targetOutcomeGuidance);
        defaultInterventionScaffold = optional(defaultInterventionScaffold);
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignServiceLine(serviceLine);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
