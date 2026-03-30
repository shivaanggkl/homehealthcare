package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchPerformanceSummaryRepository extends JpaRepository<BranchPerformanceSummary, UUID> {

    List<BranchPerformanceSummary> findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(UUID agencyId, LocalDate snapshotDate);
}
