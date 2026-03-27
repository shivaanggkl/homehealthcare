package com.homehealthcare.user.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
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

    @Transactional
    public User changeStatus(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID targetUserId,
            @NotNull UserStatus targetStatus) {
        requireAdminActor(actorMembership);

        AgencyMembership targetMembership = agencyMembershipRepository.findByUser_IdAndAgency_Id(
                        targetUserId,
                        actorMembership.getAgencyId())
                .orElseThrow(() -> new UserNotManageableInAgencyException(targetUserId, actorMembership.getAgencyId()));

        User user = targetMembership.getUser();
        applyStatus(user, targetStatus);
        User savedUser = userRepository.save(user);

        if (targetStatus == UserStatus.SUSPENDED) {
            userSessionService.revokeAllSessions(savedUser.getId(), "USER_SUSPENDED");
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

    private static void requireAdminActor(AgencyMembership actorMembership) {
        if (!actorMembership.isActive()
                || !(actorMembership.getRole() == AgencyRole.AGENCY_OWNER
                || actorMembership.getRole() == AgencyRole.BRANCH_ADMIN)) {
            throw new UnauthorizedUserStatusActorException(actorMembership.getId());
        }
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
