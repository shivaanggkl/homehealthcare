package com.homehealthcare.user.api;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.tenant.CurrentTenant;
import com.homehealthcare.user.application.UserStatusManagementService;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserStatusController {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final UserStatusManagementService userStatusManagementService;

    UserStatusController(
            CurrentTenant currentTenant,
            AgencyMembershipRepository agencyMembershipRepository,
            UserStatusManagementService userStatusManagementService) {
        this.currentTenant = currentTenant;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.userStatusManagementService = userStatusManagementService;
    }

    @PutMapping("/{userId}/status")
    UserStatusResponse changeStatus(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody ChangeUserStatusRequest request) {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));
        User savedUser = userStatusManagementService.changeStatus(actorMembership, userId, request.status());
        boolean sessionRevocationTriggered = request.status() == UserStatus.SUSPENDED || request.status() == UserStatus.DEACTIVATED;
        return new UserStatusResponse(
                savedUser.getId(),
                savedUser.getStatus(),
                sessionRevocationTriggered);
    }

    record ChangeUserStatusRequest(@NotNull UserStatus status) {
    }

    record UserStatusResponse(UUID userId, UserStatus status, boolean sessionRevocationTriggered) {
    }
}
