package com.homehealthcare.security.branch;

public enum AgencyRole {
    AGENCY_OWNER(true),
    BRANCH_ADMIN(false),
    SCHEDULER_COORDINATOR(false),
    CAREGIVER(false),
    QA_CLINICAL_REVIEWER(false),
    BILLING_BACK_OFFICE(false),
    READ_ONLY_AUDITOR(false);

    private final boolean agencyWideBranchAccess;

    AgencyRole(boolean agencyWideBranchAccess) {
        this.agencyWideBranchAccess = agencyWideBranchAccess;
    }

    public boolean hasAgencyWideBranchAccess() {
        return agencyWideBranchAccess;
    }
}
