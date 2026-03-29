package com.homehealthcare.schedulingassignment.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "caregiver_visit_assignments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaregiverVisitAssignment extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_occurrence_id", nullable = false)
    private VisitOccurrence visitOccurrence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_membership_id", nullable = false)
    private AgencyMembership assignedByMembership;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 32)
    private CaregiverAssignmentStatus assignmentStatus;

    @Column(name = "assignment_source", length = 32)
    private String assignmentSource;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Builder
    private CaregiverVisitAssignment(
            UUID id,
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Branch branch,
            AgencyMembership assignedByMembership,
            OffsetDateTime assignedAt,
            CaregiverAssignmentStatus assignmentStatus,
            String assignmentSource,
            String notes) {
        this.id = id;
        assignVisitOccurrence(visitOccurrence);
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        assignAssignedByMembership(assignedByMembership);
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.assignmentStatus = Objects.requireNonNull(assignmentStatus, "assignmentStatus must not be null");
        this.assignmentSource = assignmentSource;
        this.notes = notes;
    }

    public static CaregiverVisitAssignment create(
            VisitOccurrence visitOccurrence,
            CaregiverProfile caregiverProfile,
            Branch branch,
            AgencyMembership assignedByMembership,
            String assignmentSource,
            String notes) {
        return CaregiverVisitAssignment.builder()
                .id(UUID.randomUUID())
                .visitOccurrence(visitOccurrence)
                .caregiverProfile(caregiverProfile)
                .branch(branch)
                .assignedByMembership(assignedByMembership)
                .assignedAt(OffsetDateTime.now())
                .assignmentStatus(CaregiverAssignmentStatus.ACTIVE)
                .assignmentSource(assignmentSource)
                .notes(notes)
                .build();
    }

    public void remove() {
        this.assignmentStatus = CaregiverAssignmentStatus.REMOVED;
    }

    public void reassign() {
        this.assignmentStatus = CaregiverAssignmentStatus.REASSIGNED;
    }

    public void cancel() {
        this.assignmentStatus = CaregiverAssignmentStatus.CANCELLED;
    }

    public UUID getVisitOccurrenceId() {
        return visitOccurrence == null ? null : visitOccurrence.getId();
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile == null ? null : caregiverProfile.getId();
    }

    private void assignVisitOccurrence(VisitOccurrence visitOccurrence) {
        this.visitOccurrence = Objects.requireNonNull(visitOccurrence, "visitOccurrence must not be null");
        assignAgency(visitOccurrence.getAgency());
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        if (!Objects.equals(caregiverProfile.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("caregiverProfile must belong to the same agency as the assignment");
        }
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the assignment");
        }
        this.branch = branch;
    }

    private void assignAssignedByMembership(AgencyMembership assignedByMembership) {
        this.assignedByMembership = Objects.requireNonNull(assignedByMembership, "assignedByMembership must not be null");
        if (!Objects.equals(assignedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("assignedByMembership must belong to the same agency as the assignment");
        }
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        assignmentSource = normalizeOptional(assignmentSource);
        if (assignmentSource != null) {
            assignmentSource = assignmentSource.toUpperCase(Locale.ROOT);
        }
        notes = normalizeOptional(notes);
        assignmentStatus = Objects.requireNonNull(assignmentStatus, "assignmentStatus must not be null");
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        assignAssignedByMembership(assignedByMembership);
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }
}
