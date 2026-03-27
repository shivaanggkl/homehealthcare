package com.homehealthcare.user.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.auth.application.UserSessionManagementService;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserStatusManagementService {

    private static final String ACTOR_TYPE_AGENCY_MEMBERSHIP = "AGENCY_MEMBERSHIP";
    private static final String TARGET_TYPE_USER = "USER";

    private final UserRepository userRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final AuditEventRepository auditEventRepository;
    private final UserSessionService userSessionService;
    private final UserSessionManagementService userSessionManagementService;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    @Transactional
    public User changeStatus(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID targetUserId,
            @NotNull UserStatus targetStatus) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.MANAGE_USER_STATUS,
                UnauthorizedUserStatusActorException::new);

        AgencyMembership targetMembership = agencyMembershipRepository.findByUser_IdAndAgency_Id(
                        targetUserId,
                        actorMembership.getAgencyId())
                .orElseThrow(() -> new UserNotManageableInAgencyException(targetUserId, actorMembership.getAgencyId()));

        User user = targetMembership.getUser();
        applyStatus(user, targetStatus);
        User savedUser = userRepository.save(user);

        if (targetStatus == UserStatus.SUSPENDED || targetStatus == UserStatus.DEACTIVATED) {
            String reason = targetStatus == UserStatus.SUSPENDED ? "USER_SUSPENDED" : "USER_DEACTIVATED";
            java.util.List<java.util.UUID> revokedSessionIds = userSessionService.revokeAllSessions(savedUser.getId(), reason);
            userSessionManagementService.auditAdminRevocations(
                    actorMembership.getId(),
                    actorMembership.getUser().getEmail(),
                    actorMembership.getAgencyId(),
                    revokedSessionIds,
                    reason);
        }

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_AGENCY_MEMBERSHIP,
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                "USER_STATUS_CHANGED",
                TARGET_TYPE_USER,
                savedUser.getId(),
                actorMembership.getAgencyId(),
                "{\"newStatus\":\"" + savedUser.getStatus().name() + "\"}"));

        return savedUser;
    }

    private static void applyStatus(User user, UserStatus targetStatus) {
        switch (targetStatus) {
            case ACTIVE -> user.activate();
            case LOCKED -> user.lock();
            case SUSPENDED -> user.suspend();
            case DEACTIVATED -> user.deactivate();
            case INVITED -> throw new IllegalArgumentException("Cannot transition an existing account back to INVITED");
        }
    }
}
