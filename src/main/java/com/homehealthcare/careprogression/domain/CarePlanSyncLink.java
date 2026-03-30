package com.homehealthcare.careprogression.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.careprogression.foundation.CarePlanSyncStatus;
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
@Table(name = "careplan_sync_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CarePlanSyncLink extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_goal_id", nullable = false)
    private PatientGoal patientGoal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "careplan_identifier", nullable = false, length = 120)
    private String careplanIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    private CarePlanSyncStatus syncStatus;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "sync_source", length = 120)
    private String syncSource;

    @Builder
    private CarePlanSyncLink(
            UUID id,
            PatientGoal patientGoal,
            Branch branch,
            String careplanIdentifier,
            CarePlanSyncStatus syncStatus,
            OffsetDateTime lastSyncedAt,
            String syncSource) {
        this.id = id;
        assignPatientGoal(patientGoal);
        assignBranch(branch);
        this.careplanIdentifier = careplanIdentifier;
        this.syncStatus = Objects.requireNonNull(syncStatus, "syncStatus must not be null");
        this.lastSyncedAt = lastSyncedAt;
        this.syncSource = syncSource;
    }

    public static CarePlanSyncLink create(
            PatientGoal patientGoal,
            Branch branch,
            String careplanIdentifier,
            CarePlanSyncStatus syncStatus,
            OffsetDateTime lastSyncedAt,
            String syncSource) {
        return CarePlanSyncLink.builder()
                .id(UUID.randomUUID())
                .patientGoal(patientGoal)
                .branch(branch)
                .careplanIdentifier(careplanIdentifier)
                .syncStatus(syncStatus)
                .lastSyncedAt(lastSyncedAt)
                .syncSource(syncSource)
                .build();
    }

    public void updateState(
            Branch branch,
            String careplanIdentifier,
            CarePlanSyncStatus syncStatus,
            OffsetDateTime lastSyncedAt,
            String syncSource) {
        assignBranch(branch);
        this.careplanIdentifier = careplanIdentifier;
        this.syncStatus = Objects.requireNonNull(syncStatus, "syncStatus must not be null");
        this.lastSyncedAt = lastSyncedAt;
        this.syncSource = syncSource;
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
            throw new IllegalArgumentException("branch must belong to the same agency as the care-plan sync link");
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        careplanIdentifier = Objects.requireNonNull(careplanIdentifier, "careplanIdentifier must not be null").trim();
        syncStatus = Objects.requireNonNull(syncStatus, "syncStatus must not be null");
        syncSource = syncSource == null || syncSource.trim().isBlank() ? null : syncSource.trim();
        assignBranch(branch);
    }
}
