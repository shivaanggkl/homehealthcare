package com.homehealthcare.security.branch;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record BranchAccessContext(
        UUID agencyId,
        AgencyRole agencyRole,
        Set<UUID> assignedBranchIds) {

    public BranchAccessContext {
        assignedBranchIds = Set.copyOf(new LinkedHashSet<>(assignedBranchIds));
    }

    public boolean canAccessAllBranches() {
        return agencyRole.hasAgencyWideBranchAccess();
    }

    public boolean canAccessBranch(UUID branchId) {
        return canAccessAllBranches() || assignedBranchIds.contains(branchId);
    }
}
