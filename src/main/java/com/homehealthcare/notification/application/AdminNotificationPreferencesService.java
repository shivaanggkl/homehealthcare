package com.homehealthcare.notification.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.notification.domain.AdminNotificationPreference;
import com.homehealthcare.notification.domain.AdminNotificationPreferenceRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AdminNotificationPreferencesService {

    private static final Set<AgencyRole> ADMIN_ROLES = Set.of(AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN);
    private static final String ACTION_ADMIN_NOTIFICATION_PREFERENCES_UPDATED = "ADMIN_NOTIFICATION_PREFERENCES_UPDATED";

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AdminNotificationPreferenceRepository adminNotificationPreferenceRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public PreferenceView currentPreferences() {
        AgencyMembership actorMembership = requireAdminMembership();
        AdminNotificationPreference preference = adminNotificationPreferenceRepository.findByAgencyMembership_Id(actorMembership.getId())
                .orElseGet(() -> adminNotificationPreferenceRepository.save(AdminNotificationPreference.defaultsFor(actorMembership)));
        return PreferenceView.from(preference);
    }

    @Transactional
    public PreferenceView updatePreferences(@Valid UpdatePreferenceCommand command) {
        AgencyMembership actorMembership = requireAdminMembership();
        AdminNotificationPreference preference = adminNotificationPreferenceRepository.findByAgencyMembership_Id(actorMembership.getId())
                .orElseGet(() -> AdminNotificationPreference.defaultsFor(actorMembership));
        preference.update(
                command.emailEnabled(),
                command.failedLoginAlertsEnabled(),
                command.lockedAccountAlertsEnabled(),
                command.newAdminAlertsEnabled());
        AdminNotificationPreference saved = adminNotificationPreferenceRepository.save(preference);

        auditEventRepository.save(AuditEvent.createSuccess(
                "AGENCY_MEMBERSHIP",
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                ACTION_ADMIN_NOTIFICATION_PREFERENCES_UPDATED,
                "ADMIN_NOTIFICATION_PREFERENCE",
                saved.getId(),
                actorMembership.getAgencyId(),
                null,
                "{\"emailEnabled\":" + saved.isEmailEnabled()
                        + ",\"failedLoginAlertsEnabled\":" + saved.isFailedLoginAlertsEnabled()
                        + ",\"lockedAccountAlertsEnabled\":" + saved.isLockedAccountAlertsEnabled()
                        + ",\"newAdminAlertsEnabled\":" + saved.isNewAdminAlertsEnabled() + "}"));

        return PreferenceView.from(saved);
    }

    private AgencyMembership requireAdminMembership() {
        AgencyMembership membership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new UnauthorizedAdminNotificationPreferenceException(currentTenant.requireMembershipId()));
        if (!ADMIN_ROLES.contains(membership.getRole())) {
            throw new UnauthorizedAdminNotificationPreferenceException(membership.getId());
        }
        return membership;
    }

    public record UpdatePreferenceCommand(
            @NotNull Boolean emailEnabled,
            @NotNull Boolean failedLoginAlertsEnabled,
            @NotNull Boolean lockedAccountAlertsEnabled,
            @NotNull Boolean newAdminAlertsEnabled) {
    }

    public record PreferenceView(
            java.util.UUID membershipId,
            boolean emailEnabled,
            boolean failedLoginAlertsEnabled,
            boolean lockedAccountAlertsEnabled,
            boolean newAdminAlertsEnabled) {
        static PreferenceView from(AdminNotificationPreference preference) {
            return new PreferenceView(
                    preference.getAgencyMembership().getId(),
                    preference.isEmailEnabled(),
                    preference.isFailedLoginAlertsEnabled(),
                    preference.isLockedAccountAlertsEnabled(),
                    preference.isNewAdminAlertsEnabled());
        }
    }
}
