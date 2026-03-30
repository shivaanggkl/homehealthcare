package com.homehealthcare.patientevent.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patient.domain.Patient;
import com.homehealthcare.patientevent.foundation.Epic12PatientEventTargetType;
import com.homehealthcare.patientevent.foundation.PatientEventFollowUpStatus;
import com.homehealthcare.security.branch.AgencyRole;
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
@Table(name = "patient_event_follow_up_assignments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PatientEventFollowUpAssignment extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 48)
    private Epic12PatientEventTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_membership_id")
    private AgencyMembership ownerMembership;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_role", length = 48)
    private AgencyRole ownerRole;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "completion_at")
    private OffsetDateTime completionAt;

    @Column(name = "follow_up_note", length = 2000)
    private String followUpNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PatientEventFollowUpStatus status;

    @Builder
    private PatientEventFollowUpAssignment(
            UUID id,
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            AgencyMembership ownerMembership,
            AgencyRole ownerRole,
            OffsetDateTime assignedAt,
            OffsetDateTime dueAt,
            OffsetDateTime completionAt,
            String followUpNote,
            PatientEventFollowUpStatus status) {
        this.id = id;
        assignPatient(patient);
        assignBranch(branch);
        this.targetType = Objects.requireNonNull(targetType, "targetType must not be null");
        this.targetId = Objects.requireNonNull(targetId, "targetId must not be null");
        this.ownerMembership = ownerMembership;
        this.ownerRole = ownerRole;
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt must not be null");
        this.completionAt = completionAt;
        this.followUpNote = followUpNote;
        this.status = Objects.requireNonNull(status, "status must not be null");
        validateState();
    }

    public static PatientEventFollowUpAssignment assign(
            Patient patient,
            Branch branch,
            Epic12PatientEventTargetType targetType,
            UUID targetId,
            AgencyMembership ownerMembership,
            AgencyRole ownerRole,
            OffsetDateTime assignedAt,
            OffsetDateTime dueAt,
            String followUpNote) {
        return PatientEventFollowUpAssignment.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .branch(branch)
                .targetType(targetType)
                .targetId(targetId)
                .ownerMembership(ownerMembership)
                .ownerRole(ownerRole)
                .assignedAt(assignedAt)
                .dueAt(dueAt)
                .followUpNote(followUpNote)
                .status(PatientEventFollowUpStatus.OPEN)
                .build();
    }

    public void updateAssignment(Branch branch, AgencyMembership ownerMembership, AgencyRole ownerRole, OffsetDateTime dueAt, String followUpNote) {
        assignBranch(branch);
        this.ownerMembership = ownerMembership;
        this.ownerRole = ownerRole;
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt must not be null");
        this.followUpNote = followUpNote;
        validateState();
    }

    public void complete(OffsetDateTime completionAt, String followUpNote) {
        this.status = PatientEventFollowUpStatus.COMPLETED;
        this.completionAt = Objects.requireNonNull(completionAt, "completionAt must not be null");
        this.followUpNote = followUpNote;
        validateState();
    }

    public void cancel(String followUpNote) {
        this.status = PatientEventFollowUpStatus.CANCELLED;
        this.completionAt = null;
        this.followUpNote = followUpNote;
        validateState();
    }

    public boolean isOverdue(OffsetDateTime asOf) {
        return status == PatientEventFollowUpStatus.OPEN && dueAt.isBefore(asOf);
    }

    public UUID getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatient(Patient patient) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        assignAgency(patient.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the follow-up assignment");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        followUpNote = optional(followUpNote);
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (ownerMembership == null && ownerRole == null) {
            throw new IllegalArgumentException("follow-up assignment must have either ownerMembership or ownerRole");
        }
        if (ownerMembership != null && !Objects.equals(ownerMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("ownerMembership must belong to the same agency as the follow-up assignment");
        }
        if (dueAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException("dueAt must not be earlier than assignedAt");
        }
        if (status == PatientEventFollowUpStatus.COMPLETED && completionAt == null) {
            throw new IllegalArgumentException("completed follow-up assignments must record completionAt");
        }
        if (status != PatientEventFollowUpStatus.COMPLETED && completionAt != null) {
            throw new IllegalArgumentException("only completed follow-up assignments may store completionAt");
        }
    }

    private static String optional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
