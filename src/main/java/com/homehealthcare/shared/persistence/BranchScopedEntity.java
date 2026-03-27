package com.homehealthcare.shared.persistence;

import com.homehealthcare.branch.domain.Branch;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BranchScopedEntity extends AgencyScopedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    protected void assignBranch(Branch branch) {
        this.branch = branch;
        assignAgency(branch == null ? null : branch.getAgency());
    }

    public UUID getBranchId() {
        return branch == null ? null : branch.getId();
    }
}
