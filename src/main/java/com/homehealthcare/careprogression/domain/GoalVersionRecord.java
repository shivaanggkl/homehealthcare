package com.homehealthcare.careprogression.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "goal_version_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalVersionRecord extends AgencyScopedEntity {

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
    @JoinColumn(name = "changed_by_membership_id")
    private AgencyMembership changedByMembership;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "change_type", nullable = false, length = 80)
    private String changeType;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "snapshot_json", nullable = false, length = 8000)
    private String snapshotJson;

    @Builder
    private GoalVersionRecord(
            UUID id,
            PatientGoal patientGoal,
            Branch branch,
            AgencyMembership changedByMembership,
            int versionNumber,
            String changeType,
            OffsetDateTime changedAt,
            String snapshotJson) {
        this.id = id;
        assignPatientGoal(patientGoal);
        assignBranch(branch);
        assignChangedByMembership(changedByMembership);
        this.versionNumber = versionNumber;
        this.changeType = changeType;
        this.changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        this.snapshotJson = snapshotJson;
    }

    public static GoalVersionRecord create(
            PatientGoal patientGoal,
            Branch branch,
            AgencyMembership changedByMembership,
            int versionNumber,
            String changeType,
            OffsetDateTime changedAt,
            String snapshotJson) {
        return GoalVersionRecord.builder()
                .id(UUID.randomUUID())
                .patientGoal(patientGoal)
                .branch(branch)
                .changedByMembership(changedByMembership)
                .versionNumber(versionNumber)
                .changeType(changeType)
                .changedAt(changedAt)
                .snapshotJson(snapshotJson)
                .build();
    }

    public UUID getPatientGoalId() {
        return patientGoal == null ? null : patientGoal.getId();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignPatientGoal(PatientGoal patientGoal) {
        this.patientGoal = Objects.requireNonNull(patientGoal, "patientGoal must not be null");
        assignAgency(patientGoal.getAgency());
    }

    private void assignBranch(Branch branch) {
        if (branch != null && !Objects.equals(branch.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("branch must belong to the same agency as the goal version");
        }
        this.branch = branch;
    }

    private void assignChangedByMembership(AgencyMembership changedByMembership) {
        if (changedByMembership != null && !Objects.equals(changedByMembership.getAgencyId(), getAgencyId())) {
            throw new IllegalArgumentException("changedByMembership must belong to the same agency as the goal version");
        }
        this.changedByMembership = changedByMembership;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (versionNumber <= 0) {
            throw new IllegalArgumentException("versionNumber must be positive");
        }
        changeType = Objects.requireNonNull(changeType, "changeType must not be null").trim();
        changedAt = Objects.requireNonNull(changedAt, "changedAt must not be null");
        snapshotJson = Objects.requireNonNull(snapshotJson, "snapshotJson must not be null").trim();
        assignBranch(branch);
        assignChangedByMembership(changedByMembership);
    }
}
