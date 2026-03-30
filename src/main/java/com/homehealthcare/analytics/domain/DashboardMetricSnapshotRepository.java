package com.homehealthcare.analytics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardMetricSnapshotRepository extends JpaRepository<DashboardMetricSnapshot, UUID> {

    List<DashboardMetricSnapshot> findAllByAgency_IdAndSnapshotDateOrderByCapturedAtDesc(UUID agencyId, LocalDate snapshotDate);

    List<DashboardMetricSnapshot> findAllByAgency_IdAndMetricDefinition_MetricTypeOrderBySnapshotDateDescCapturedAtDesc(
            UUID agencyId,
            com.homehealthcare.analytics.foundation.AnalyticsDashboardMetricType metricType);
}
