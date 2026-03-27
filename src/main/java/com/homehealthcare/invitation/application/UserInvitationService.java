package com.homehealthcare.invitation.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.invitation.domain.UserInvitation;
import com.homehealthcare.invitation.domain.UserInvitationRepository;
import com.homehealthcare.invitation.domain.UserInvitationStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.user.domain.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserInvitationService {

    private static final String ACTOR_TYPE_AGENCY_MEMBERSHIP = "AGENCY_MEMBERSHIP";
    private static final String ACTION_USER_INVITED = "USER_INVITED";
    private static final String TARGET_TYPE_USER_INVITATION = "USER_INVITATION";
    private static final Duration DEFAULT_INVITATION_TTL = Duration.ofDays(7);

    private final AgencyMembershipRepository agencyMembershipRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final AuditEventRepository auditEventRepository;
    private final InvitationEmailSender invitationEmailSender;

    @Transactional
    public InviteUserResult inviteUser(
            @NotNull AgencyMembership actorMembership,
            @Valid InviteUserCommand command) {
        requireInviterCanManageUsers(actorMembership);

        String normalizedEmail = command.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.save(User.invite(
                        command.firstName(),
                        command.lastName(),
                        normalizedEmail,
                        command.phone())));

        AgencyMembership membership = agencyMembershipRepository.findByUser_IdAndAgency_Id(
                        user.getId(),
                        actorMembership.getAgencyId())
                .map(existing -> updateExistingMembership(existing, command.role()))
                .orElseGet(() -> agencyMembershipRepository.save(
                        AgencyMembership.grant(user, actorMembership.getAgency(), command.role())));

        synchronizeBranchAssignments(membership, actorMembership.getAgencyId(), command.branchIds());

        userInvitationRepository.findFirstByAgency_IdAndEmailAndStatusOrderByCreatedAtDesc(
                        actorMembership.getAgencyId(),
                        normalizedEmail,
                        UserInvitationStatus.PENDING)
                .ifPresent(existing -> userInvitationRepository.save(cancelInvitation(existing)));

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(DEFAULT_INVITATION_TTL);
        String token = UUID.randomUUID().toString();
        UserInvitation invitation = userInvitationRepository.save(UserInvitation.issue(
                actorMembership,
                membership,
                user,
                normalizedEmail,
                token,
                expiresAt));

        invitationEmailSender.send(new InvitationEmailSender.InvitationEmail(
                invitation.getId(),
                invitation.getAgencyId(),
                normalizedEmail,
                token,
                expiresAt));

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_AGENCY_MEMBERSHIP,
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                ACTION_USER_INVITED,
                TARGET_TYPE_USER_INVITATION,
                invitation.getId(),
                actorMembership.getAgencyId(),
                "{\"email\":\"" + normalizedEmail
                        + "\",\"role\":\"" + membership.getRole().name()
                        + "\",\"branchCount\":" + command.branchIds().size()
                        + ",\"expiresAt\":\"" + expiresAt + "\"}"));

        return new InviteUserResult(invitation, membership);
    }

    private static void requireInviterCanManageUsers(AgencyMembership actorMembership) {
        if (!actorMembership.isActive()
                || !(actorMembership.getRole() == AgencyRole.AGENCY_OWNER
                || actorMembership.getRole() == AgencyRole.BRANCH_ADMIN)) {
            throw new UnauthorizedInvitationActorException(actorMembership.getId());
        }
    }

    private AgencyMembership updateExistingMembership(AgencyMembership membership, AgencyRole role) {
        if (membership.isActive() && membership.getUser().getStatus() != UserStatus.INVITED) {
            throw new UserAlreadyAgencyMemberException(membership.getUserId(), membership.getAgencyId());
        }
        membership.activate();
        membership.changeRole(role);
        return agencyMembershipRepository.save(membership);
    }

    private void synchronizeBranchAssignments(AgencyMembership membership, UUID agencyId, Set<UUID> branchIds) {
        Set<UUID> normalizedBranchIds = new HashSet<>(branchIds);
        List<BranchAssignment> existingAssignments = branchAssignmentRepository
                .findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                        membership.getId(),
                        com.homehealthcare.branchassignment.domain.BranchAssignmentStatus.ACTIVE);

        for (UUID branchId : normalizedBranchIds) {
            Branch branch = branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                    .orElseThrow(() -> new InvalidInvitationBranchException(branchId, agencyId));

            branchAssignmentRepository.findByAgencyMembership_IdAndBranch_Id(membership.getId(), branchId)
                    .ifPresentOrElse(existing -> {
                        existing.activate();
                        branchAssignmentRepository.save(existing);
                    }, () -> branchAssignmentRepository.save(BranchAssignment.assign(membership, branch)));
        }

        for (BranchAssignment assignment : existingAssignments) {
            if (!normalizedBranchIds.contains(assignment.getBranchId())) {
                assignment.deactivate();
                branchAssignmentRepository.save(assignment);
            }
        }
    }

    private UserInvitation cancelInvitation(UserInvitation invitation) {
        invitation.cancel();
        return invitation;
    }

    public record InviteUserCommand(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank @Email String email,
            String phone,
            @NotNull AgencyRole role,
            @NotNull Set<UUID> branchIds) {
    }

    public record InviteUserResult(
            UserInvitation invitation,
            AgencyMembership membership) {
    }
}
