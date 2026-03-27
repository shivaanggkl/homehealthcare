package com.homehealthcare.branchassignment.domain;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.shared.persistence.BranchScopedEntity;
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
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "branch_assignments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchAssignment extends BranchScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_membership_id", nullable = false)
    private AgencyMembership agencyMembership;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BranchAssignmentStatus status;

    @Builder
    private BranchAssignment(
            UUID id,
            AgencyMembership agencyMembership,
            Branch branch,
            BranchAssignmentStatus status) {
        validateSameAgency(agencyMembership, branch);
        this.id = id;
        this.agencyMembership = agencyMembership;
        assignBranch(branch);
        this.status = status;
    }

    public static BranchAssignment assign(AgencyMembership agencyMembership, Branch branch) {
        if (!agencyMembership.isActive()) {
            throw new IllegalArgumentException("Branch assignment requires an active agency membership");
        }

        return BranchAssignment.builder()
                .id(UUID.randomUUID())
                .agencyMembership(agencyMembership)
                .branch(branch)
                .status(BranchAssignmentStatus.ACTIVE)
                .build();
    }

    public void activate() {
        if (!agencyMembership.isActive()) {
            throw new IllegalStateException("Cannot activate branch assignment for inactive agency membership");
        }
        this.status = BranchAssignmentStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = BranchAssignmentStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == BranchAssignmentStatus.ACTIVE;
    }

    public UUID getAgencyMembershipId() {
        return agencyMembership == null ? null : agencyMembership.getId();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        validateSameAgency(agencyMembership, getBranch());
    }

    private static void validateSameAgency(AgencyMembership agencyMembership, Branch branch) {
        if (agencyMembership == null || branch == null) {
            return;
        }
        if (!Objects.equals(agencyMembership.getAgencyId(), branch.getAgencyId())) {
            throw new IllegalArgumentException("Branch assignment requires membership and branch from the same agency");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BranchAssignment branchAssignment)) {
            return false;
        }
        return id != null && Objects.equals(id, branchAssignment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
