package com.homehealthcare.user.application;

import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.tenant.CurrentTenant;
import com.homehealthcare.user.domain.UserStatus;
import jakarta.persistence.criteria.JoinType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UserDirectoryService {

    private final CurrentTenant currentTenant;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;

    @Transactional(readOnly = true)
    public Page<UserDirectoryEntry> viewDirectory(UserDirectoryFilter filter, Pageable pageable) {
        AgencyMembership actorMembership = agencyMembershipRepository.findById(currentTenant.requireMembershipId())
                .orElseThrow(() -> new UnauthorizedUserDirectoryActorException(currentTenant.requireMembershipId()));
        requireDirectoryAccess(actorMembership);

        Page<AgencyMembership> memberships = agencyMembershipRepository.findAll(buildSpecification(
                actorMembership.getAgencyId(),
                filter), pageable);

        Map<UUID, List<String>> branchNamesByMembershipId = loadBranchNames(memberships.getContent().stream()
                .map(AgencyMembership::getId)
                .toList());

        return memberships.map(membership -> new UserDirectoryEntry(
                membership.getUserId(),
                membership.getId(),
                membership.getUser().getFirstName(),
                membership.getUser().getLastName(),
                membership.getUser().getEmail(),
                membership.getUser().getPhone(),
                membership.getUser().getStatus(),
                membership.getRole(),
                membership.getUser().getLastLoginAt(),
                membership.getUser().isMfaEnabled(),
                branchNamesByMembershipId.getOrDefault(membership.getId(), List.of())));
    }

    private static void requireDirectoryAccess(AgencyMembership actorMembership) {
        if (!actorMembership.isActive()
                || !(actorMembership.getRole() == AgencyRole.AGENCY_OWNER
                || actorMembership.getRole() == AgencyRole.BRANCH_ADMIN)) {
            throw new UnauthorizedUserDirectoryActorException(actorMembership.getId());
        }
    }

    private Specification<AgencyMembership> buildSpecification(UUID agencyId, UserDirectoryFilter filter) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("agency").get("id"), agencyId));

            var userJoin = root.join("user");

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("firstName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("lastName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("email")), pattern)));
            }

            if (filter.userStatus() != null) {
                predicates.add(criteriaBuilder.equal(userJoin.get("status"), filter.userStatus()));
            }

            if (filter.role() != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), filter.role()));
            }

            if (filter.branchId() != null) {
                var branchAssignmentJoin = root.join("branchAssignments", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(branchAssignmentJoin.get("branch").get("id"), filter.branchId()));
                predicates.add(criteriaBuilder.equal(branchAssignmentJoin.get("status"), BranchAssignmentStatus.ACTIVE));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Map<UUID, List<String>> loadBranchNames(Collection<UUID> membershipIds) {
        if (membershipIds.isEmpty()) {
            return Map.of();
        }

        return branchAssignmentRepository.findAllByAgencyMembership_IdInAndStatusOrderByBranch_NameAsc(
                        membershipIds,
                        BranchAssignmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(
                        BranchAssignment::getAgencyMembershipId,
                        Collectors.mapping(
                                assignment -> assignment.getBranch().getName(),
                                Collectors.collectingAndThen(Collectors.toList(), List::copyOf))));
    }

    public record UserDirectoryFilter(
            String search,
            UserStatus userStatus,
            AgencyRole role,
            UUID branchId) {
    }

    public record UserDirectoryEntry(
            UUID userId,
            UUID membershipId,
            String firstName,
            String lastName,
            String email,
            String phone,
            UserStatus userStatus,
            AgencyRole role,
            java.time.OffsetDateTime lastLoginAt,
            boolean mfaEnabled,
            List<String> branchNames) {
    }
}
