package com.homehealthcare.security.branch;

import com.homehealthcare.security.authorization.AgencyPermission;
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

    public boolean canAccessAllBranches(AgencyPermission permission) {
        return permission.isAgencyWideFor(agencyRole)
                || (agencyRole.hasAgencyWideBranchAccess() && permission.isAllowedFor(agencyRole));
    }

    public boolean canAccessBranch(AgencyPermission permission, UUID branchId) {
        if (!permission.isAllowedFor(agencyRole)) {
            return false;
        }
        if (canAccessAllBranches(permission)) {
            return true;
        }
        return permission.requiresAssignedBranchFor(agencyRole) && assignedBranchIds.contains(branchId);
    }
}
