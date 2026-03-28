package com.homehealthcare.branchpolicy.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchPolicyRepository extends JpaRepository<BranchPolicy, UUID> {

    boolean existsByBranch_IdAndPolicyKey(UUID branchId, String policyKey);

    boolean existsByBranch_IdAndPolicyKeyAndIdNot(UUID branchId, String policyKey, UUID id);

    Optional<BranchPolicy> findByBranch_IdAndPolicyKey(UUID branchId, String policyKey);

    List<BranchPolicy> findAllByAgency_IdOrderByPolicyKeyAsc(UUID agencyId);

    default Optional<BranchPolicy> findEffectivePolicy(UUID branchId, String policyKey, OffsetDateTime timestamp) {
        OffsetDateTime probe = timestamp == null ? OffsetDateTime.now() : timestamp;
        return findByBranch_IdAndPolicyKey(branchId, policyKey)
                .filter(policy -> policy.isEffectiveAt(probe));
    }
}
