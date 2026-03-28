package com.homehealthcare.security.authorization;

import com.homehealthcare.security.branch.AgencyRole;
import java.util.EnumSet;
import java.util.Set;

public enum AgencyPermission {
    VIEW_AGENCY_SETTINGS(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_AGENCY_SETTINGS(true, AgencyRole.AGENCY_OWNER),
    VIEW_USER_DIRECTORY(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    INVITE_USER(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    EDIT_USER_PROFILE(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_USER_STATUS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_AGENCY_MFA_POLICY(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    VIEW_AUDIT_LOG(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.BILLING_BACK_OFFICE, AgencyRole.READ_ONLY_AUDITOR),
    VIEW_BRANCH(false,
            Set.of(AgencyRole.AGENCY_OWNER, AgencyRole.BILLING_BACK_OFFICE, AgencyRole.READ_ONLY_AUDITOR),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.CAREGIVER, AgencyRole.QA_CLINICAL_REVIEWER)),
    EDIT_BRANCH(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN));

    private final boolean sensitive;
    private final Set<AgencyRole> agencyWideRoles;
    private final Set<AgencyRole> branchScopedRoles;

    AgencyPermission(boolean sensitive, AgencyRole... allowedRoles) {
        this.sensitive = sensitive;
        this.agencyWideRoles = EnumSet.of(allowedRoles[0], allowedRoles);
        this.branchScopedRoles = Set.of();
    }

    AgencyPermission(boolean sensitive, Set<AgencyRole> agencyWideRoles, Set<AgencyRole> branchScopedRoles) {
        this.sensitive = sensitive;
        this.agencyWideRoles = EnumSet.copyOf(agencyWideRoles);
        this.branchScopedRoles = EnumSet.copyOf(branchScopedRoles);
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public boolean isAllowedFor(AgencyRole role) {
        return agencyWideRoles.contains(role) || branchScopedRoles.contains(role);
    }

    public boolean isAgencyWideFor(AgencyRole role) {
        return agencyWideRoles.contains(role);
    }

    public boolean requiresAssignedBranchFor(AgencyRole role) {
        return branchScopedRoles.contains(role);
    }
}
