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
    }

    @Test
    void disallowedRoleIsDeniedSensitivePermission() {
        AgencyMembership caregiver = membership(AgencyRole.CAREGIVER, true);

        assertThat(AgencyPermission.MANAGE_USER_STATUS.isSensitive()).isTrue();
        assertThat(guard.hasPermission(caregiver, AgencyPermission.MANAGE_USER_STATUS)).isFalse();
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
