package com.homehealthcare.user.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.notification.application.AdminNotificationEventType;
import com.homehealthcare.notification.application.AdminSecurityNotificationService;
import com.homehealthcare.platform.audit.domain.AuditEvent;
import com.homehealthcare.platform.audit.domain.AuditEventRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserProfileAssignmentService {

    private static final String ACTOR_TYPE_AGENCY_MEMBERSHIP = "AGENCY_MEMBERSHIP";
    private static final String ACTION_USER_PROFILE_ASSIGNMENTS_UPDATED = "USER_PROFILE_ASSIGNMENTS_UPDATED";
    private static final String ACTION_USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    private static final String ACTION_USER_BRANCH_ASSIGNMENTS_CHANGED = "USER_BRANCH_ASSIGNMENTS_CHANGED";
    private static final String TARGET_TYPE_USER = "USER";

    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchRepository branchRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AuditEventRepository auditEventRepository;
    private final AdminSecurityNotificationService adminSecurityNotificationService;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    @Transactional
    public UpdatedUserResult updateUser(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID targetUserId,
            @Valid UpdateUserCommand command) {
        agencyAuthorizationGuard.requirePermission(
                actorMembership,
                AgencyPermission.EDIT_USER_PROFILE,
                UnauthorizedUserEditActorException::new);

        AgencyMembership targetMembership = agencyMembershipRepository.findByUser_IdAndAgency_Id(
                        targetUserId,
                        actorMembership.getAgencyId())
                .orElseThrow(() -> new UserNotManageableInAgencyException(targetUserId, actorMembership.getAgencyId()));

        enforceProtectedRoleRules(actorMembership, targetMembership, command.role());

        AgencyRole previousRole = targetMembership.getRole();
        Set<UUID> previousBranchIds = activeBranchIds(targetMembership.getId());

        User targetUser = targetMembership.getUser();
        targetUser.updateProfile(command.firstName(), command.lastName(), command.phone());
        targetMembership.changeRole(command.role());
        agencyMembershipRepository.save(targetMembership);

        synchronizeBranchAssignments(targetMembership, actorMembership.getAgencyId(), command.branchIds());
        Set<UUID> updatedBranchIds = activeBranchIds(targetMembership.getId());

        auditEventRepository.save(AuditEvent.create(
                ACTOR_TYPE_AGENCY_MEMBERSHIP,
                actorMembership.getId(),
                actorMembership.getUser().getEmail(),
                ACTION_USER_PROFILE_ASSIGNMENTS_UPDATED,
                TARGET_TYPE_USER,
                targetUser.getId(),
                actorMembership.getAgencyId(),
                "{\"role\":\"" + command.role().name()
                        + "\",\"branchCount\":" + command.branchIds().size()
                        + ",\"phoneUpdated\":" + (command.phone() != null) + "}"));

        if (previousRole != command.role()) {
            auditEventRepository.save(AuditEvent.createSuccess(
                    ACTOR_TYPE_AGENCY_MEMBERSHIP,
                    actorMembership.getId(),
                    actorMembership.getUser().getEmail(),
                    ACTION_USER_ROLE_CHANGED,
                    TARGET_TYPE_USER,
                    targetUser.getId(),
                    actorMembership.getAgencyId(),
                    null,
                    "{\"previousRole\":\"" + previousRole.name()
                            + "\",\"newRole\":\"" + command.role().name() + "\"}"));

            if ((command.role() == AgencyRole.AGENCY_OWNER || command.role() == AgencyRole.BRANCH_ADMIN)
                    && previousRole != AgencyRole.AGENCY_OWNER
                    && previousRole != AgencyRole.BRANCH_ADMIN) {
                adminSecurityNotificationService.notifyAgencyAdmins(new AdminSecurityNotificationService.CriticalAdminNotification(
                        actorMembership.getAgencyId(),
                        null,
                        AdminNotificationEventType.NEW_ADMIN_CREATED,
                        "New admin created",
                        targetUser.getEmail() + " was granted the " + command.role().name() + " role.",
                        "new-admin-role-" + targetUser.getId(),
                        java.time.OffsetDateTime.now().plusDays(1),
                        TARGET_TYPE_USER,
                        targetUser.getId()));
            }
        }

        if (!previousBranchIds.equals(updatedBranchIds)) {
            auditEventRepository.save(AuditEvent.createSuccess(
                    ACTOR_TYPE_AGENCY_MEMBERSHIP,
                    actorMembership.getId(),
                    actorMembership.getUser().getEmail(),
                    ACTION_USER_BRANCH_ASSIGNMENTS_CHANGED,
                    TARGET_TYPE_USER,
                    targetUser.getId(),
                    actorMembership.getAgencyId(),
                    null,
                    "{\"previousBranchIds\":" + toMetadataJsonArray(previousBranchIds)
                            + ",\"newBranchIds\":" + toMetadataJsonArray(updatedBranchIds) + "}"));
        }

        List<String> branchNames = branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                        targetMembership.getId(),
                        BranchAssignmentStatus.ACTIVE)
                .stream()
                .map(assignment -> assignment.getBranch().getName())
                .toList();

        return new UpdatedUserResult(
                targetUser.getId(),
                targetMembership.getId(),
                targetUser.getFirstName(),
                targetUser.getLastName(),
                targetUser.getPhone(),
                targetMembership.getRole(),
                branchNames);
    }

    private Set<UUID> activeBranchIds(UUID membershipId) {
        return branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                        membershipId,
                        BranchAssignmentStatus.ACTIVE)
                .stream()
                .map(BranchAssignment::getBranchId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static String toMetadataJsonArray(Set<UUID> branchIds) {
        return branchIds.stream()
                .map(branchId -> "\"" + branchId + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static void enforceProtectedRoleRules(
            AgencyMembership actorMembership,
            AgencyMembership targetMembership,
            AgencyRole targetRole) {
        if (actorMembership.getRole() == AgencyRole.BRANCH_ADMIN) {
            if (targetMembership.getRole() == AgencyRole.AGENCY_OWNER || targetRole == AgencyRole.AGENCY_OWNER) {
                throw new ProtectedUserEditException("Branch Admin cannot edit or assign Agency Owner role");
            }
        }
    }

    private void synchronizeBranchAssignments(AgencyMembership membership, UUID agencyId, Set<UUID> branchIds) {
        Set<UUID> normalizedBranchIds = new HashSet<>(branchIds);
        List<BranchAssignment> existingAssignments = branchAssignmentRepository
                .findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                        membership.getId(),
                        BranchAssignmentStatus.ACTIVE);

        for (UUID branchId : normalizedBranchIds) {
            Branch branch = branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                    .orElseThrow(() -> new com.homehealthcare.invitation.application.InvalidInvitationBranchException(branchId, agencyId));

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

    public record UpdateUserCommand(
            @NotBlank String firstName,
            @NotBlank String lastName,
            String phone,
            @NotNull AgencyRole role,
            @NotNull Set<UUID> branchIds) {
    }

    public record UpdatedUserResult(
            UUID userId,
            UUID membershipId,
            String firstName,
            String lastName,
            String phone,
            AgencyRole role,
            List<String> branchNames) {
    }
}
