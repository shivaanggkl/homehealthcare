package com.homehealthcare.careprogression.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.careprogression.foundation.GoalInterventionLifecycleStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
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
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "goal_interventions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalIntervention extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_goal_id", nullable = false)
    private PatientGoal patientGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_membership_id")
    private AgencyMembership ownerMembership;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GoalInterventionLifecycleStatus status;

    @Column(name = "derived_from_template", nullable = false)
    private boolean derivedFromTemplate;

    @Builder
    private GoalIntervention(
            UUID id,
            PatientGoal patientGoal,
            Branch branch,
            AgencyMembership ownerMembership,
            String title,
            String description,
            LocalDate targetDate,
            GoalInterventionLifecycleStatus status,
            boolean derivedFromTemplate) {
        this.id = id;
        assignPatientGoal(patientGoal);
        assignBranch(branch);
        assignOwnerMembership(ownerMembership);
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.derivedFromTemplate = derivedFromTemplate;
    }

    public static GoalIntervention create(
            PatientGoal patientGoal,
            Branch branch,
            AgencyMembership ownerMembership,
            String title,
            String description,
            LocalDate targetDate,
            boolean derivedFromTemplate) {
        return GoalIntervention.builder()
                .id(UUID.randomUUID())
                .patientGoal(patientGoal)
                .branch(branch)
                .ownerMembership(ownerMembership)
                .title(title)
                .description(description)
                .targetDate(targetDate)
                .status(GoalInterventionLifecycleStatus.ACTIVE)
                .derivedFromTemplate(derivedFromTemplate)
                .build();
    }

    public void updateDetails(
            Branch branch,
            AgencyMembership ownerMembership,
            String title,
            String description,
            LocalDate targetDate,
            GoalInterventionLifecycleStatus status) {
        assignBranch(branch);
        assignOwnerMembership(ownerMembership);
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void deactivate() {
        this.status = GoalInterventionLifecycleStatus.INACTIVE;
    }

    public UUID getPatientGoalId() {
        return patientGoal == null ? null : patientGoal.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getOwnerMembershipId() {
        return ownerMembership == null ? null : ownerMembership.getId();
    }

    private void assignPatientGoal(PatientGoal patientGoal) {
        this.patientGoal = Objects.requireNonNull(patientGoal, "patientGoal must not be null");
        assignAgency(patientGoal.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the intervention");
        }
        this.branch = branch;
    }

    private void assignOwnerMembership(AgencyMembership ownerMembership) {
        if (ownerMembership != null && !Objects.equals(ownerMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("ownerMembership must belong to the same agency as the intervention");
        }
        this.ownerMembership = ownerMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        title = required(title);
        description = optional(description);
        status = Objects.requireNonNull(status, "status must not be null");
        assignBranch(branch);
        assignOwnerMembership(ownerMembership);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
