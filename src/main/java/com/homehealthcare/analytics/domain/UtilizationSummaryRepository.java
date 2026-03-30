package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilizationSummaryRepository extends JpaRepository<UtilizationSummary, UUID> {

    List<UtilizationSummary> findAllByAgency_IdAndSnapshotDateOrderByScheduledMinutesDesc(UUID agencyId, LocalDate snapshotDate);
}
