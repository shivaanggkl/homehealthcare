package com.homehealthcare.user.api;

import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.CurrentTenant;
import com.homehealthcare.user.application.UserProfileAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserProfileAssignmentController {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final UserProfileAssignmentService userProfileAssignmentService;

    UserProfileAssignmentController(
            CurrentTenant currentTenant,
            AgencyMembershipRepository agencyMembershipRepository,
            BranchAssignmentRepository branchAssignmentRepository,
            UserProfileAssignmentService userProfileAssignmentService) {
        this.currentTenant = currentTenant;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.userProfileAssignmentService = userProfileAssignmentService;
    }

    @PutMapping("/{userId}")
    UpdatedUserResponse updateUser(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));

        UserProfileAssignmentService.UpdatedUserResult result = userProfileAssignmentService.updateUser(
                actorMembership,
                userId,
                new UserProfileAssignmentService.UpdateUserCommand(
                        request.firstName(),
                        request.lastName(),
                        request.phone(),
                        request.role(),
                        request.branchIds()));

        List<BranchAssignment> assignments = branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                result.membershipId(),
                BranchAssignmentStatus.ACTIVE);

        return new UpdatedUserResponse(
                result.userId(),
                result.membershipId(),
                result.firstName(),
                result.lastName(),
                result.phone(),
                result.role(),
                assignments.stream().map(assignment -> assignment.getBranch().getId()).toList(),
                result.branchNames());
    }

    record UpdateUserRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String phone,
            @NotNull AgencyRole role,
            @NotNull Set<UUID> branchIds) {
    }

    record UpdatedUserResponse(
            UUID userId,
            UUID membershipId,
            String firstName,
            String lastName,
            String phone,
            AgencyRole role,
            List<UUID> branchIds,
            List<String> branchNames) {
    }
}
