package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequiredDocumentationRequirementRepository extends JpaRepository<RequiredDocumentationRequirement, UUID> {

    Optional<RequiredDocumentationRequirement> findByIdAndAgency_Id(UUID id, UUID agencyId);

    boolean existsByAgency_IdAndRequirementCode(UUID agencyId, String requirementCode);

    List<RequiredDocumentationRequirement> findAllByAgency_IdOrderByRequirementCodeAsc(UUID agencyId);
}
