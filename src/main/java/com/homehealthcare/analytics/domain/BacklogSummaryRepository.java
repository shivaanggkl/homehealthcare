package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BacklogSummaryRepository extends JpaRepository<BacklogSummary, UUID> {

    List<BacklogSummary> findAllByAgency_IdAndSnapshotDateOrderByEvaluatedAtDesc(UUID agencyId, LocalDate snapshotDate);
}
