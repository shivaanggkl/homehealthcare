package com.homehealthcare.evv.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceToleranceRuleRepository extends JpaRepository<GeofenceToleranceRule, UUID> {

    Optional<GeofenceToleranceRule> findFirstByAgency_IdAndBranch_IdAndActiveTrueOrderByCreatedAtDesc(UUID agencyId, UUID branchId);

    Optional<GeofenceToleranceRule> findFirstByAgency_IdAndBranchIsNullAndActiveTrueOrderByCreatedAtDesc(UUID agencyId);
}
