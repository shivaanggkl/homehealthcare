package com.homehealthcare.analytics.domain;

import com.homehealthcare.analytics.foundation.AnalyticsUtilizationPosture;
import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
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
@Table(name = "utilization_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtilizationSummary extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caregiver_profile_id", nullable = false)
    private CaregiverProfile caregiverProfile;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "assigned_visit_count", nullable = false)
    private int assignedVisitCount;

    @Column(name = "completed_visit_count", nullable = false)
    private int completedVisitCount;

    @Column(name = "scheduled_minutes", nullable = false)
    private int scheduledMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "posture", nullable = false, length = 24)
    private AnalyticsUtilizationPosture posture;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    @Builder
    private UtilizationSummary(
            UUID id,
            Branch branch,
            CaregiverProfile caregiverProfile,
            LocalDate snapshotDate,
            int assignedVisitCount,
            int completedVisitCount,
            int scheduledMinutes,
            AnalyticsUtilizationPosture posture,
            OffsetDateTime evaluatedAt) {
        this.id = id;
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        this.snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        this.assignedVisitCount = assignedVisitCount;
        this.completedVisitCount = completedVisitCount;
        this.scheduledMinutes = scheduledMinutes;
        this.posture = Objects.requireNonNull(posture, "posture must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public static UtilizationSummary create(
            Branch branch,
            CaregiverProfile caregiverProfile,
            LocalDate snapshotDate,
            int assignedVisitCount,
            int completedVisitCount,
            int scheduledMinutes,
            AnalyticsUtilizationPosture posture,
            OffsetDateTime evaluatedAt) {
        return UtilizationSummary.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .caregiverProfile(caregiverProfile)
                .snapshotDate(snapshotDate)
                .assignedVisitCount(assignedVisitCount)
                .completedVisitCount(completedVisitCount)
                .scheduledMinutes(scheduledMinutes)
                .posture(posture)
                .evaluatedAt(evaluatedAt)
                .build();
    }

    public void recalculate(
            Branch branch,
            int assignedVisitCount,
            int completedVisitCount,
            int scheduledMinutes,
            AnalyticsUtilizationPosture posture,
            OffsetDateTime evaluatedAt) {
        assignBranch(branch);
        this.assignedVisitCount = assignedVisitCount;
        this.completedVisitCount = completedVisitCount;
        this.scheduledMinutes = scheduledMinutes;
        this.posture = Objects.requireNonNull(posture, "posture must not be null");
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        validateState();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    public UUID getCaregiverProfileId() {
        return caregiverProfile.getId();
    }

    private void assignCaregiverProfile(CaregiverProfile caregiverProfile) {
        this.caregiverProfile = Objects.requireNonNull(caregiverProfile, "caregiverProfile must not be null");
        assignAgency(caregiverProfile.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the utilization summary");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        snapshotDate = Objects.requireNonNull(snapshotDate, "snapshotDate must not be null");
        posture = Objects.requireNonNull(posture, "posture must not be null");
        evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        assignCaregiverProfile(caregiverProfile);
        assignBranch(branch);
        validateState();
    }

    private void validateState() {
        if (assignedVisitCount < 0 || completedVisitCount < 0 || scheduledMinutes < 0) {
            throw new IllegalArgumentException("utilization values must be greater than or equal to 0");
        }
    }
}
