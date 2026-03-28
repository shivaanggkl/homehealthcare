package com.homehealthcare.alertrule.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    boolean existsByAgency_IdAndBranch_IdAndName(UUID agencyId, UUID branchId, String name);

    boolean existsByAgency_IdAndBranchIsNullAndName(UUID agencyId, String name);

    boolean existsByAgency_IdAndBranch_IdAndNameAndIdNot(UUID agencyId, UUID branchId, String name, UUID id);

    boolean existsByAgency_IdAndBranchIsNullAndNameAndIdNot(UUID agencyId, String name, UUID id);
}
