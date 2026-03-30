package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceChecklistDefinitionRepository extends JpaRepository<ComplianceChecklistDefinition, UUID> {

    Optional<ComplianceChecklistDefinition> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByAgency_IdAndItemCode(UUID agencyId, String itemCode);

    List<ComplianceChecklistDefinition> findAllByAgency_IdOrderByItemCodeAsc(UUID agencyId);
}
