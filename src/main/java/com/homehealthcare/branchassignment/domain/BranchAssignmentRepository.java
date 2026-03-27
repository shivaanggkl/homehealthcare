package com.homehealthcare.branchassignment.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchAssignmentRepository extends JpaRepository<BranchAssignment, UUID> {

    boolean existsByAgencyMembership_IdAndBranch_Id(UUID agencyMembershipId, UUID branchId);

    Optional<BranchAssignment> findByAgencyMembership_IdAndBranch_Id(UUID agencyMembershipId, UUID branchId);

    List<BranchAssignment> findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
            UUID agencyMembershipId,
            BranchAssignmentStatus status);

    List<BranchAssignment> findAllByAgencyMembership_IdInAndStatusOrderByBranch_NameAsc(
            Collection<UUID> agencyMembershipIds,
            BranchAssignmentStatus status);
}
