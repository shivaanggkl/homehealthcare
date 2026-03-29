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
    VIEW_AGENCY_CONFIGURATION(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_AGENCY_CONFIGURATION(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    VIEW_TEMPLATE_CONFIGURATION(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_TEMPLATE_CONFIGURATION(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    VIEW_WORKFORCE_CONFIGURATION(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_WORKFORCE_CONFIGURATION(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    VIEW_BRANCH_POLICY(false,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN)),
    MANAGE_BRANCH_POLICY(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN)),
    VIEW_ALERT_RULE(false,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN)),
    MANAGE_ALERT_RULE(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN)),
    VIEW_COMPENSATION_SETTINGS(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    MANAGE_COMPENSATION_SETTINGS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN),
    VIEW_PATIENT_DIRECTORY(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.QA_CLINICAL_REVIEWER),
    MANAGE_PATIENT_DEMOGRAPHICS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR),
    MANAGE_PATIENT_CONTACTS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR),
    MANAGE_PATIENT_ADDRESS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR),
    MANAGE_PATIENT_ELIGIBILITY(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR),
    MANAGE_PATIENT_DIAGNOSES(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.QA_CLINICAL_REVIEWER),
    MANAGE_PATIENT_PAYER_LINKAGE(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.BILLING_BACK_OFFICE),
    MANAGE_PATIENT_AUTHORIZATIONS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.BILLING_BACK_OFFICE),
    VIEW_PATIENT_ATTACHMENTS(false, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.QA_CLINICAL_REVIEWER, AgencyRole.BILLING_BACK_OFFICE),
    MANAGE_PATIENT_ATTACHMENTS(true, AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR, AgencyRole.QA_CLINICAL_REVIEWER),
    VIEW_WORKFORCE_DIRECTORY(false,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    MANAGE_CAREGIVER_PROFILES(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    MANAGE_CAREGIVER_CREDENTIALS(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    MANAGE_CAREGIVER_AVAILABILITY(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    MANAGE_CAREGIVER_UNAVAILABILITY(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    VIEW_CAREGIVER_PERFORMANCE(false,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    VIEW_SCHEDULING_WORKSPACE(false,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    MANAGE_SCHEDULE_VISITS(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    ASSIGN_CAREGIVERS(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    MANAGE_OPEN_SHIFTS(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    RESCHEDULE_VISITS(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    CANCEL_VISITS(true,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
    VIEW_SCHEDULE_CONFLICTS(false,
            Set.of(AgencyRole.AGENCY_OWNER),
            Set.of(AgencyRole.BRANCH_ADMIN, AgencyRole.SCHEDULER_COORDINATOR)),
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
