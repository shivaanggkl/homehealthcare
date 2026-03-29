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
