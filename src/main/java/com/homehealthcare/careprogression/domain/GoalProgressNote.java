package com.homehealthcare.careprogression.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.careprogression.foundation.GoalProgressNoteLifecycleStatus;
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
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "goal_progress_notes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalProgressNote extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_goal_id", nullable = false)
    private PatientGoal patientGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_intervention_id")
    private GoalIntervention goalIntervention;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "captured_by_membership_id", nullable = false)
    private AgencyMembership capturedByMembership;

    @Column(name = "note_text", nullable = false, length = 4000)
    private String noteText;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "progression_summary", length = 2000)
    private String progressionSummary;

    @Column(name = "status_impact", length = 100)
    private String statusImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private GoalProgressNoteLifecycleStatus lifecycleStatus;

    @Builder
    private GoalProgressNote(
            UUID id,
            PatientGoal patientGoal,
            GoalIntervention goalIntervention,
            Branch branch,
            AgencyMembership capturedByMembership,
            String noteText,
            OffsetDateTime capturedAt,
            String progressionSummary,
            String statusImpact,
            GoalProgressNoteLifecycleStatus lifecycleStatus) {
        this.id = id;
        assignPatientGoal(patientGoal);
        assignGoalIntervention(goalIntervention);
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
        this.noteText = noteText;
        this.capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        this.progressionSummary = progressionSummary;
        this.statusImpact = statusImpact;
        this.lifecycleStatus = Objects.requireNonNull(lifecycleStatus, "lifecycleStatus must not be null");
    }

    public static GoalProgressNote create(
            PatientGoal patientGoal,
            GoalIntervention goalIntervention,
            Branch branch,
            AgencyMembership capturedByMembership,
            String noteText,
            OffsetDateTime capturedAt,
            String progressionSummary,
            String statusImpact) {
        return GoalProgressNote.builder()
                .id(UUID.randomUUID())
                .patientGoal(patientGoal)
                .goalIntervention(goalIntervention)
                .branch(branch)
                .capturedByMembership(capturedByMembership)
                .noteText(noteText)
                .capturedAt(capturedAt)
                .progressionSummary(progressionSummary)
                .statusImpact(statusImpact)
                .lifecycleStatus(GoalProgressNoteLifecycleStatus.FINALIZED)
                .build();
    }

    public UUID getPatientGoalId() {
        return patientGoal == null ? null : patientGoal.getId();
    }

    public UUID getGoalInterventionId() {
        return goalIntervention == null ? null : goalIntervention.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatientGoal(PatientGoal patientGoal) {
        this.patientGoal = Objects.requireNonNull(patientGoal, "patientGoal must not be null");
        assignAgency(patientGoal.getAgency());
    }

    private void assignGoalIntervention(GoalIntervention goalIntervention) {
        if (goalIntervention != null) {
            if (!Objects.equals(goalIntervention.getAgencyId(), getAgencyId())) {
                throw new IllegalArgumentException("goalIntervention must belong to the same agency as the progress note");
            }
            if (!Objects.equals(goalIntervention.getPatientGoalId(), patientGoal.getId())) {
                throw new IllegalArgumentException("goalIntervention must belong to the same patient goal as the progress note");
            }
        }
        this.goalIntervention = goalIntervention;
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the progress note");
        }
        this.branch = branch;
    }

    private void assignCapturedByMembership(AgencyMembership capturedByMembership) {
        if (!Objects.equals(capturedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("capturedByMembership must belong to the same agency as the progress note");
        }
        this.capturedByMembership = Objects.requireNonNull(capturedByMembership, "capturedByMembership must not be null");
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        noteText = required(noteText);
        progressionSummary = optional(progressionSummary);
        statusImpact = optional(statusImpact);
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt must not be null");
        lifecycleStatus = Objects.requireNonNull(lifecycleStatus, "lifecycleStatus must not be null");
        assignGoalIntervention(goalIntervention);
        assignBranch(branch);
        assignCapturedByMembership(capturedByMembership);
    }

    private static String required(String value) {
        return Objects.requireNonNull(value, "value must not be null").trim();
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
