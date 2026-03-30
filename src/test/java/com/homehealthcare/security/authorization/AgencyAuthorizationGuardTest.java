package com.homehealthcare.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.application.UnauthorizedUserDirectoryActorException;
import com.homehealthcare.user.domain.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgencyAuthorizationGuardTest {

    private final AgencyAuthorizationGuard guard = new AgencyAuthorizationGuard();

    @Test
    void allowedRoleHasConfiguredPermission() {
        AgencyMembership branchAdmin = membership(AgencyRole.BRANCH_ADMIN, true);

        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_USER_DIRECTORY)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_AGENCY_MFA_POLICY)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_BRANCH_POLICY)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_AGENCY_CONFIGURATION)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_PATIENT_DIRECTORY)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_PATIENT_ATTACHMENTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_WORKFORCE_DIRECTORY)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_CAREGIVER_AVAILABILITY)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_SCHEDULING_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.ASSIGN_CAREGIVERS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_DOCUMENTATION_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_DOCUMENTATION_TEMPLATES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.DRAFT_VISIT_DOCUMENTATION)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_MESSAGING_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.SEND_SECURE_MESSAGES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_STAFF_GROUPS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.SEND_BRANCH_BROADCASTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_MESSAGE_ESCALATIONS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_REVIEW_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_EXCEPTION_QUEUE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.ASSIGN_REVIEW_WORK)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.PERFORM_REVIEW_DECISIONS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.REQUEST_REVIEW_SIGNOFF)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_REVIEW_AUDIT_CONTEXT)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_PATIENT_ACKNOWLEDGMENTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_CERTIFICATION_PERIODS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.CREATE_INCIDENT_RECORDS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_INFECTION_RECORDS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_WOUND_RECORDS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.LINK_PATIENT_EVENT_EVIDENCE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.ASSIGN_PATIENT_EVENT_FOLLOW_UP)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.ESCALATE_PATIENT_EVENTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.RESOLVE_PATIENT_EVENTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_GOAL_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_GOAL_TEMPLATES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_PATIENT_GOALS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_GOAL_INTERVENTIONS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.ADD_GOAL_PROGRESS_NOTES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_GOAL_STATE_TRANSITIONS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_CAREPLAN_SYNC)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.RECALCULATE_REVENUE_READINESS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.MANAGE_REVENUE_EXCEPTION_FLAGS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.GENERATE_REVENUE_EXPORTS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_ANALYTICS_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_BRANCH_PERFORMANCE_METRICS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_CAREGIVER_UTILIZATION_METRICS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_QA_BACKLOG_METRICS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_REVENUE_READINESS_METRICS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.VIEW_COMPLIANCE_EXCEPTION_METRICS)).isTrue();
        assertThat(guard.hasPermission(branchAdmin, AgencyPermission.REFRESH_DASHBOARD_METRICS)).isTrue();
    }

    @Test
    void disallowedRoleIsDeniedSensitivePermission() {
        AgencyMembership caregiver = membership(AgencyRole.CAREGIVER, true);

        assertThat(AgencyPermission.MANAGE_USER_STATUS.isSensitive()).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_USER_STATUS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_TEMPLATE_CONFIGURATION)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_PATIENT_DIRECTORY)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_PATIENT_ATTACHMENTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_WORKFORCE_DIRECTORY)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_CAREGIVER_PERFORMANCE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_SCHEDULING_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.CANCEL_VISITS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_OWN_MOBILE_VISITS)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.EXECUTE_OWN_VISITS)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.SUBMIT_MOBILE_VISIT_DOCUMENTATION)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.UPLOAD_MOBILE_VISIT_ARTIFACTS)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.CREATE_MOBILE_INCIDENTS)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_MOBILE_MESSAGES)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.SEND_MOBILE_MESSAGES)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_OWN_EVV)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.SUBMIT_OWN_EVV)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_EVV_EXCEPTIONS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_MISSED_VISITS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.RESOLVE_MISSED_VISITS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.RECEIVE_EVV_NOTIFICATIONS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_DOCUMENTATION_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_VISIT_DOCUMENTATION)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.DRAFT_VISIT_DOCUMENTATION)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.SUBMIT_VISIT_DOCUMENTATION)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_DOCUMENTATION_TEMPLATES)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.GENERATE_PRINTABLE_DOCUMENTATION_SUMMARY)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_MESSAGING_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.SEND_SECURE_MESSAGES)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_STAFF_GROUPS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.SEND_BRANCH_BROADCASTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_MESSAGE_ESCALATIONS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_REVIEW_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_EXCEPTION_QUEUE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.ASSIGN_REVIEW_WORK)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.PERFORM_REVIEW_DECISIONS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.REQUEST_REVIEW_SIGNOFF)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_REVIEW_AUDIT_CONTEXT)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_REQUIRED_DOCUMENTATION_RULES)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_PATIENT_ACKNOWLEDGMENTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_CERTIFICATION_PERIODS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_PATIENT_RISK_REMINDERS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.CREATE_INCIDENT_RECORDS)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_INFECTION_RECORDS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_WOUND_RECORDS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.LINK_PATIENT_EVENT_EVIDENCE)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.ASSIGN_PATIENT_EVENT_FOLLOW_UP)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.ESCALATE_PATIENT_EVENTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.RESOLVE_PATIENT_EVENTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_GOAL_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_GOAL_TEMPLATES)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_PATIENT_GOALS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_GOAL_INTERVENTIONS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.ADD_GOAL_PROGRESS_NOTES)).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_GOAL_STATE_TRANSITIONS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_CAREPLAN_SYNC)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.RECALCULATE_REVENUE_READINESS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_REVENUE_EXCEPTION_FLAGS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.GENERATE_REVENUE_EXPORTS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_ANALYTICS_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_BRANCH_PERFORMANCE_METRICS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_CAREGIVER_UTILIZATION_METRICS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_QA_BACKLOG_METRICS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_REVENUE_READINESS_METRICS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.VIEW_COMPLIANCE_EXCEPTION_METRICS)).isFalse();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.REFRESH_DASHBOARD_METRICS)).isFalse();
        assertThatThrownBy(() -> guard.requirePermission(
                caregiver,
                AgencyPermission.MANAGE_USER_STATUS,
                UnauthorizedUserDirectoryActorException::new))
                .isInstanceOf(UnauthorizedUserDirectoryActorException.class);
    }

    @Test
    void inactiveMembershipIsDeniedEvenWhenRoleWouldNormallyBeAllowed() {
        AgencyMembership owner = membership(AgencyRole.AGENCY_OWNER, false);

        assertThat(guard.hasPermission(owner, AgencyPermission.INVITE_USER)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.MANAGE_ALERT_RULE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.MANAGE_PATIENT_DEMOGRAPHICS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.MANAGE_SCHEDULE_VISITS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.RESOLVE_MISSED_VISITS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.MANAGE_DOCUMENTATION_TEMPLATES)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_MESSAGING_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.SEND_SECURE_MESSAGES)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_REVIEW_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.PERFORM_REVIEW_DECISIONS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.RECALCULATE_COMPLIANCE_STATUS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.RESOLVE_PATIENT_EVENTS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_GOAL_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.MANAGE_CAREPLAN_SYNC)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.GENERATE_REVENUE_EXPORTS)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.VIEW_ANALYTICS_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(owner, AgencyPermission.REFRESH_DASHBOARD_METRICS)).isFalse();
    }

    @Test
    void branchScopedEpic2PermissionsRemainUnavailableToAgencyWideReadOnlyRoles() {
        AgencyMembership auditor = membership(AgencyRole.READ_ONLY_AUDITOR, true);

        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_AUDIT_LOG)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_BRANCH_POLICY)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.MANAGE_COMPENSATION_SETTINGS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_PATIENT_DIRECTORY)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_PATIENT_ATTACHMENTS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_WORKFORCE_DIRECTORY)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_SCHEDULE_CONFLICTS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_MISSED_VISITS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_DOCUMENTATION_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_MESSAGING_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_REVIEW_WORKSPACE)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_REVIEW_AUDIT_CONTEXT)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_COMPLIANCE_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_COMPLIANCE_DASHBOARD)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.MANAGE_COMPLIANCE_CHECKLISTS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_PATIENT_EVENT_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.CREATE_INCIDENT_RECORDS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_GOAL_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.ADD_GOAL_PROGRESS_NOTES)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_REVENUE_READINESS_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.RECALCULATE_REVENUE_READINESS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.GENERATE_REVENUE_EXPORTS)).isFalse();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_AUTHORIZATION_USAGE_SUMMARIES)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_PAYER_SERVICE_SUMMARIES)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_ANALYTICS_WORKSPACE)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_BRANCH_PERFORMANCE_METRICS)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_CAREGIVER_UTILIZATION_METRICS)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_QA_BACKLOG_METRICS)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_REVENUE_READINESS_METRICS)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.VIEW_COMPLIANCE_EXCEPTION_METRICS)).isTrue();
        assertThat(guard.hasPermission(auditor, AgencyPermission.REFRESH_DASHBOARD_METRICS)).isFalse();
    }

    private AgencyMembership membership(AgencyRole role, boolean active) {
        AgencyMembership membership = AgencyMembership.grant(
                User.invite("Test", "User", UUID.randomUUID() + "@northstar.example", null),
                Agency.create("North Star Home Care", UUID.randomUUID().toString(), "America/Chicago", "ops@northstar.example"),
                role);
        if (!active) {
            membership.deactivate();
        }
        return membership;
    }
}
