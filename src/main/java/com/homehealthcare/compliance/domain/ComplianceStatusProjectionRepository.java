package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceStatusProjectionRepository extends JpaRepository<ComplianceStatusProjection, UUID> {

    List<ComplianceStatusProjection> findAllByAgency_IdOrderByEvaluatedAtDesc(UUID agencyId);

    List<ComplianceStatusProjection> findAllByPatient_IdOrderByEvaluatedAtDesc(UUID patientId);
}
