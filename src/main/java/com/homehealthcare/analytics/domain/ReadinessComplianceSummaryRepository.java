package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadinessComplianceSummaryRepository extends JpaRepository<ReadinessComplianceSummary, UUID> {

    List<ReadinessComplianceSummary> findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(UUID agencyId, LocalDate snapshotDate);
}
