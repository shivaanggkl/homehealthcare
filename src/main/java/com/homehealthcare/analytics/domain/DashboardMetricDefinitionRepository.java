package com.homehealthcare.analytics.domain;

import com.homehealthcare.analytics.foundation.AnalyticsDashboardMetricType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardMetricDefinitionRepository extends JpaRepository<DashboardMetricDefinition, UUID> {

    Optional<DashboardMetricDefinition> findByAgency_IdAndMetricType(UUID agencyId, AnalyticsDashboardMetricType metricType);

    List<DashboardMetricDefinition> findAllByAgency_IdOrderByMetricTypeAsc(UUID agencyId);
}
