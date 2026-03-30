package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricTrendSnapshotRepository extends JpaRepository<MetricTrendSnapshot, UUID> {

    List<MetricTrendSnapshot> findAllByAgency_IdAndSnapshotDateOrderByCalculatedAtDesc(UUID agencyId, LocalDate snapshotDate);
}
