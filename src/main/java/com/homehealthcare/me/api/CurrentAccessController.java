package com.homehealthcare.me.api;

import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.CurrentTenant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/access")
class CurrentAccessController {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;

    CurrentAccessController(
            CurrentTenant currentTenant,
            AgencyMembershipRepository agencyMembershipRepository,
            BranchAssignmentRepository branchAssignmentRepository,
            AgencyAuthorizationGuard agencyAuthorizationGuard) {
        this.currentTenant = currentTenant;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
    }

    @GetMapping
    CurrentAccessResponse currentAccess() {
        AgencyMembership membership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new IllegalStateException("Current membership was not found"));

        List<String> permissions = Arrays.stream(AgencyPermission.values())
                .filter(permission -> agencyAuthorizationGuard.hasPermission(membership, permission))
                .map(Enum::name)
                .toList();

        Set<UUID> assignedBranchIds = branchAssignmentRepository
                .findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                        membership.getId(),
                        BranchAssignmentStatus.ACTIVE)
                .stream()
                .map(assignment -> assignment.getBranch().getId())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        return new CurrentAccessResponse(
                membership.getUserId(),
                membership.getAgencyId(),
                membership.getId(),
                membership.getRole(),
                branchScopeFor(membership.getRole()),
                List.copyOf(assignedBranchIds),
                permissions);
    }

    private static BranchScopeType branchScopeFor(AgencyRole role) {
        return switch (role) {
            case AGENCY_OWNER -> BranchScopeType.AGENCY_WIDE;
            case BILLING_BACK_OFFICE, READ_ONLY_AUDITOR -> BranchScopeType.AGENCY_WIDE_READ;
            default -> BranchScopeType.ASSIGNED_BRANCHES;
        };
    }

    enum BranchScopeType {
        AGENCY_WIDE,
        AGENCY_WIDE_READ,
        ASSIGNED_BRANCHES
    }

    record CurrentAccessResponse(
            UUID userId,
            UUID agencyId,
            UUID membershipId,
            AgencyRole role,
            BranchScopeType branchScope,
            List<UUID> assignedBranchIds,
            List<String> permissions) {
    }
}
