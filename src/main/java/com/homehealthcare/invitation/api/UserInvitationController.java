package com.homehealthcare.invitation.api;

import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.invitation.application.AcceptInvitationService;
import com.homehealthcare.invitation.application.UserInvitationService;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
class UserInvitationController {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final UserInvitationService userInvitationService;
    private final AcceptInvitationService acceptInvitationService;

    UserInvitationController(
            CurrentTenant currentTenant,
            AgencyMembershipRepository agencyMembershipRepository,
            BranchAssignmentRepository branchAssignmentRepository,
            UserInvitationService userInvitationService,
            AcceptInvitationService acceptInvitationService) {
        this.currentTenant = currentTenant;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.userInvitationService = userInvitationService;
        this.acceptInvitationService = acceptInvitationService;
    }

    @PostMapping("/api/users/invitations")
    InvitationResponse invite(@Valid @RequestBody InviteUserRequest request) {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));

        UserInvitationService.InviteUserResult result = userInvitationService.inviteUser(
                actorMembership,
                new UserInvitationService.InviteUserCommand(
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.phone(),
                        request.role(),
                        request.branchIds()));

        return toResponse(result.invitation(), result.membership().getId());
    }

    @GetMapping("/api/invitations/{token}")
    InvitationDetailsResponse invitation(@PathVariable("token") String token) {
        AcceptInvitationService.InvitationDetails details = acceptInvitationService.getInvitationDetails(token);
        List<BranchAssignment> branchAssignments = branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                details.membershipId(),
                BranchAssignmentStatus.ACTIVE);
        return new InvitationDetailsResponse(
                details.invitationId(),
                details.agencyId(),
                details.email(),
                details.firstName(),
                details.lastName(),
                details.phone(),
                details.role(),
                details.expiresAt(),
                branchAssignments.stream().map(assignment -> assignment.getBranch().getId()).toList(),
                branchAssignments.stream().map(assignment -> assignment.getBranch().getName()).toList());
    }

    @PostMapping("/api/invitations/{token}/accept")
    AcceptedInvitationResponse accept(
            @PathVariable("token") String token,
            @Valid @RequestBody AcceptInvitationRequest request) {
        AcceptInvitationService.AcceptedInvitationResult result = acceptInvitationService.acceptInvitation(
                token,
                new AcceptInvitationService.AcceptInvitationCommand(
                        request.firstName(),
                        request.lastName(),
                        request.phone(),
                        request.password()));
        return new AcceptedInvitationResponse(
                result.invitationId(),
                result.userId(),
                result.membershipId(),
                result.agencyId());
    }

    private InvitationResponse toResponse(
            com.homehealthcare.invitation.domain.UserInvitation invitation,
            UUID membershipId) {
        List<BranchAssignment> assignments = branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                membershipId,
                BranchAssignmentStatus.ACTIVE);

        return new InvitationResponse(
                invitation.getId(),
                invitation.getAgencyId(),
                invitation.getAgencyMembershipId(),
                invitation.getUser().getId(),
                invitation.getEmail(),
                invitation.getAgencyMembership().getRole(),
                invitation.getExpiresAt(),
                assignments.stream().map(assignment -> assignment.getBranch().getId()).toList(),
                assignments.stream().map(assignment -> assignment.getBranch().getName()).toList());
    }

    record InviteUserRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            String phone,
            @NotNull AgencyRole role,
            @NotNull Set<UUID> branchIds) {
    }

    record InvitationResponse(
            UUID invitationId,
            UUID agencyId,
            UUID membershipId,
            UUID userId,
            String email,
            AgencyRole role,
            java.time.OffsetDateTime expiresAt,
            List<UUID> branchIds,
            List<String> branchNames) {
    }

    record InvitationDetailsResponse(
            UUID invitationId,
            UUID agencyId,
            String email,
            String firstName,
            String lastName,
            String phone,
            AgencyRole role,
            java.time.OffsetDateTime expiresAt,
            List<UUID> branchIds,
            List<String> branchNames) {
    }

    record AcceptInvitationRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String phone,
            @NotBlank String password) {
    }

    record AcceptedInvitationResponse(
            UUID invitationId,
            UUID userId,
            UUID membershipId,
            UUID agencyId) {
    }
}
