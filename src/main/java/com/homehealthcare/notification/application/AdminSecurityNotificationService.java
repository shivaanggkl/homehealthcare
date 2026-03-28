package com.homehealthcare.notification.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import com.homehealthcare.notification.domain.AdminNotificationPreference;
import com.homehealthcare.notification.domain.AdminNotificationPreferenceRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.platform.email.SecurityAlertEmailSender;
import com.homehealthcare.platform.email.SystemEmailTemplateService;
import com.homehealthcare.platform.email.TemplatedEmail;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSecurityNotificationService {

    private static final Set<AgencyRole> ADMIN_ROLES = Set.of(AgencyRole.AGENCY_OWNER, AgencyRole.BRANCH_ADMIN);
    private static final String ACTION_ADMIN_SECURITY_NOTIFICATION_SENT = "ADMIN_SECURITY_NOTIFICATION_SENT";

    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AdminNotificationPreferenceRepository adminNotificationPreferenceRepository;
    private final ObjectProvider<SecurityAlertEmailSender> securityAlertEmailSenderProvider;
    private final SystemEmailTemplateService systemEmailTemplateService;
    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void notifyAgencyAdmins(CriticalAdminNotification notification) {
        SecurityAlertEmailSender sender = securityAlertEmailSenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }

        for (AgencyMembership adminMembership : agencyMembershipRepository.findAllByAgency_IdAndStatus(
                notification.agencyId(),
                AgencyMembershipStatus.ACTIVE)) {
            if (!ADMIN_ROLES.contains(adminMembership.getRole())) {
                continue;
            }
            if (adminMembership.getUser().getStatus() != UserStatus.ACTIVE) {
                continue;
            }

            AdminNotificationPreference preference = adminNotificationPreferenceRepository.findByAgencyMembership_Id(adminMembership.getId())
                    .orElseGet(() -> adminNotificationPreferenceRepository.save(AdminNotificationPreference.defaultsFor(adminMembership)));
            if (!isEnabled(preference, notification.type())) {
                continue;
            }

            TemplatedEmail email = systemEmailTemplateService.composeSecurityAlertEmail(
                    adminMembership.getUser().getFirstName(),
                    notification.subject(),
                    notification.message(),
                    notification.reference(),
                    notification.expiresAt());
            sender.send(new SecurityAlertEmailSender.SecurityAlertEmail(
                    adminMembership.getUser().getEmail(),
                    email.subject(),
                    email.textBody(),
                    email.htmlBody(),
                    email.actionUrl(),
                    email.expiresAt()));

            auditEventRepository.save(AuditEvent.createSuccess(
                    "AGENCY_MEMBERSHIP",
                    adminMembership.getId(),
                    adminMembership.getUser().getEmail(),
                    ACTION_ADMIN_SECURITY_NOTIFICATION_SENT,
                    notification.targetType(),
                    notification.targetId(),
                    notification.agencyId(),
                    notification.branchId(),
                    "{\"notificationType\":\"" + notification.type().name()
                            + "\",\"recipientMembershipId\":\"" + adminMembership.getId()
                            + "\",\"recipientEmail\":\"" + adminMembership.getUser().getEmail() + "\"}"));
        }
    }

    private static boolean isEnabled(AdminNotificationPreference preference, AdminNotificationEventType type) {
        if (!preference.isEmailEnabled()) {
            return false;
        }
        return switch (type) {
            case REPEATED_FAILED_LOGIN -> preference.isFailedLoginAlertsEnabled();
            case LOCKED_ACCOUNT -> preference.isLockedAccountAlertsEnabled();
            case NEW_ADMIN_CREATED -> preference.isNewAdminAlertsEnabled();
        };
    }

    public record CriticalAdminNotification(
            UUID agencyId,
            UUID branchId,
            AdminNotificationEventType type,
            String subject,
            String message,
            String reference,
            OffsetDateTime expiresAt,
            String targetType,
            UUID targetId) {
    }
}
