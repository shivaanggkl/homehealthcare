package com.homehealthcare.compliance.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceChecklistResultRepository extends JpaRepository<ComplianceChecklistResult, UUID> {

    List<ComplianceChecklistResult> findAllByAgency_IdOrderByEvaluatedAtDesc(UUID agencyId);

    List<ComplianceChecklistResult> findAllByPatient_IdOrderByEvaluatedAtDesc(UUID patientId);
}
