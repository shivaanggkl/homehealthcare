package com.homehealthcare.evv.domain;

import com.homehealthcare.branch.domain.Branch;
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
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "evv_geofence_tolerance_rules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeofenceToleranceRule extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "rule_name", nullable = false, length = 120)
    private String ruleName;

    @Column(name = "tolerance_meters", nullable = false)
    private int toleranceMeters;

    @Column(name = "warning_buffer_meters", nullable = false)
    private int warningBufferMeters;

    @Column(name = "hard_block_outside_tolerance", nullable = false)
    private boolean hardBlockOutsideTolerance;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private GeofenceToleranceRule(
            UUID id,
            Branch branch,
            String ruleName,
            int toleranceMeters,
            int warningBufferMeters,
            boolean hardBlockOutsideTolerance,
            boolean active) {
        this.id = id;
        assignBranch(branch);
        this.ruleName = ruleName;
        this.toleranceMeters = toleranceMeters;
        this.warningBufferMeters = warningBufferMeters;
        this.hardBlockOutsideTolerance = hardBlockOutsideTolerance;
        this.active = active;
        validate();
    }

    public static GeofenceToleranceRule createAgencyDefault(
            com.homehealthcare.agency.domain.Agency agency,
            String ruleName,
            int toleranceMeters,
            int warningBufferMeters,
            boolean hardBlockOutsideTolerance) {
        GeofenceToleranceRule rule = new GeofenceToleranceRule();
        rule.id = UUID.randomUUID();
        rule.assignAgency(agency);
        rule.ruleName = ruleName;
        rule.toleranceMeters = toleranceMeters;
        rule.warningBufferMeters = warningBufferMeters;
        rule.hardBlockOutsideTolerance = hardBlockOutsideTolerance;
        rule.active = true;
        rule.validate();
        return rule;
    }

    public static GeofenceToleranceRule createBranchOverride(
            Branch branch,
            String ruleName,
            int toleranceMeters,
            int warningBufferMeters,
            boolean hardBlockOutsideTolerance) {
        return GeofenceToleranceRule.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .ruleName(ruleName)
                .toleranceMeters(toleranceMeters)
                .warningBufferMeters(warningBufferMeters)
                .hardBlockOutsideTolerance(hardBlockOutsideTolerance)
                .active(true)
                .build();
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }

    private void assignBranch(Branch branch) {
        if (branch != null) {
            assignAgency(branch.getAgency());
        }
        this.branch = branch;
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        ruleName = Objects.requireNonNull(ruleName, "ruleName must not be null").trim();
        validate();
    }

    private void validate() {
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("ruleName must not be blank");
        }
        if (toleranceMeters < 0) {
            throw new IllegalArgumentException("toleranceMeters must not be negative");
        }
        if (warningBufferMeters < 0) {
            throw new IllegalArgumentException("warningBufferMeters must not be negative");
        }
    }
}
