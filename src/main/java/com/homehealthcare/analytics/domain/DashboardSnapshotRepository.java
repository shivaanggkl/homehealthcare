package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardSnapshotRepository extends JpaRepository<DashboardSnapshot, UUID> {

    List<DashboardSnapshot> findAllByAgency_IdAndSnapshotDateOrderByGeneratedAtDesc(UUID agencyId, LocalDate snapshotDate);

    List<DashboardSnapshot> findAllByAgency_IdOrderBySnapshotDateDescGeneratedAtDesc(UUID agencyId);
}
