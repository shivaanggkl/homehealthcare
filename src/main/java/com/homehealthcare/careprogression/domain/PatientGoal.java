package com.homehealthcare.careprogression.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.careprogression.foundation.PatientGoalLifecycleStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
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
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "patient_goals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientGoal extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_template_id")
    private GoalTemplate goalTemplate;

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
    private PatientGoalLifecycleStatus status;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Builder
    private PatientGoal(
            UUID id,
            Patient patient,
            Branch branch,
            GoalTemplate goalTemplate,
            AgencyMembership ownerMembership,
            String title,
            String description,
            LocalDate targetDate,
            PatientGoalLifecycleStatus status,
            OffsetDateTime resolvedAt) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        assignGoalTemplate(goalTemplate);
        assignOwnerMembership(ownerMembership);
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.resolvedAt = resolvedAt;
        validateState();
    }

    public static PatientGoal create(
            Patient patient,
            Branch branch,
            GoalTemplate goalTemplate,
            AgencyMembership ownerMembership,
            String title,
            String description,
            LocalDate targetDate) {
        return PatientGoal.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .goalTemplate(goalTemplate)
                .ownerMembership(ownerMembership)
                .title(title)
                .description(description)
                .targetDate(targetDate)
                .status(PatientGoalLifecycleStatus.ACTIVE)
                .build();
    }

    public void updateDetails(
            Branch branch,
            GoalTemplate goalTemplate,
            AgencyMembership ownerMembership,
            String title,
            String description,
            LocalDate targetDate) {
        assignBranch(branch);
        assignGoalTemplate(goalTemplate);
        assignOwnerMembership(ownerMembership);
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        validateState();
    }

    public void transitionState(PatientGoalLifecycleStatus newStatus, OffsetDateTime changedAt) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (status == newStatus) {
            return;
        }
        if (status != PatientGoalLifecycleStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active goals may transition to a terminal state");
        }
        if (newStatus == PatientGoalLifecycleStatus.ACTIVE) {
            throw new IllegalArgumentException("Goals may not transition back to ACTIVE");
        }
        this.status = newStatus;
        this.resolvedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        validateState();
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getGoalTemplateId() {
        return goalTemplate == null ? null : goalTemplate.getId();
    }

    public UUID getOwnerMembershipId() {
        return ownerMembership == null ? null : ownerMembership.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the patient goal");
        }
        this.branch = branch;
    }

    private void assignGoalTemplate(GoalTemplate goalTemplate) {
        if (goalTemplate != null && !Objects.equals(goalTemplate.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("goalTemplate must belong to the same agency as the patient goal");
        }
        this.goalTemplate = goalTemplate;
    }

    private void assignOwnerMembership(AgencyMembership ownerMembership) {
        if (ownerMembership != null && !Objects.equals(ownerMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("ownerMembership must belong to the same agency as the patient goal");
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
        assignGoalTemplate(goalTemplate);
        assignOwnerMembership(ownerMembership);
        validateState();
    }

    private void validateState() {
        if (status == PatientGoalLifecycleStatus.ACTIVE && resolvedAt != null) {
            throw new IllegalArgumentException("active goals must not have resolvedAt");
        }
        if (status != PatientGoalLifecycleStatus.ACTIVE && resolvedAt == null) {
            throw new IllegalArgumentException("terminal goals must record resolvedAt");
        }
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
