package com.homehealthcare.security.branch;

import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.tenant.CurrentTenant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentBranchAccess {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;

    public Optional<BranchAccessContext> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        return extractPrincipal(authentication)
                .map(principal -> new BranchAccessContext(
                        principal.currentAgencyId(),
                        principal.agencyRole(),
                        principal.assignedBranchIds()))
                .or(this::resolveFromPersistedMembership);
    }

    private Optional<BranchAccessPrincipal> extractPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof BranchAccessPrincipal principal) {
            return Optional.of(principal);
        }
        if (authentication.getDetails() instanceof BranchAccessPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    private Optional<BranchAccessContext> resolveFromPersistedMembership() {
        return currentTenant.get()
                .flatMap(tenantContext -> agencyMembershipRepository.findById(tenantContext.membershipId()))
                .filter(AgencyMembership::isActive)
                .map(membership -> new BranchAccessContext(
                        membership.getAgencyId(),
                        membership.getRole(),
                        resolveAssignedBranchIds(membership.getId())));
    }

    private Set<UUID> resolveAssignedBranchIds(UUID membershipId) {
        return branchAssignmentRepository.findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                        membershipId,
                        BranchAssignmentStatus.ACTIVE)
                .stream()
                .map(BranchAssignment::getBranchId)
                .collect(Collectors.toUnmodifiableSet());
    }
}
