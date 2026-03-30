package com.homehealthcare.analytics.domain;

import com.homehealthcare.agency.domain.Agency;
import com.homehealthcare.analytics.foundation.AnalyticsDashboardMetricType;
import com.homehealthcare.analytics.foundation.AnalyticsMetricScope;
import com.homehealthcare.analytics.foundation.AnalyticsRefreshMode;
import com.homehealthcare.shared.persistence.AgencyScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "dashboard_metric_definitions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardMetricDefinition extends AgencyScopedEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 48)
    private AnalyticsDashboardMetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_scope", nullable = false, length = 24)
    private AnalyticsMetricScope metricScope;

    @Column(name = "metric_name", nullable = false, length = 160)
    private String metricName;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "source_domain_key", nullable = false, length = 80)
    private String sourceDomainKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "refresh_mode", nullable = false, length = 24)
    private AnalyticsRefreshMode refreshMode;

    @Column(name = "max_staleness_minutes", nullable = false)
    private int maxStalenessMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private DashboardMetricDefinition(
            UUID id,
            Agency agency,
            AnalyticsDashboardMetricType metricType,
            AnalyticsMetricScope metricScope,
            String metricName,
            String description,
            String sourceDomainKey,
            AnalyticsRefreshMode refreshMode,
            int maxStalenessMinutes,
            boolean active) {
        this.id = id;
        assignAgency(Objects.requireNonNull(agency, "agency must not be null"));
        this.metricType = Objects.requireNonNull(metricType, "metricType must not be null");
        this.metricScope = Objects.requireNonNull(metricScope, "metricScope must not be null");
        this.metricName = metricName;
        this.description = description;
        this.sourceDomainKey = sourceDomainKey;
        this.refreshMode = Objects.requireNonNull(refreshMode, "refreshMode must not be null");
        this.maxStalenessMinutes = maxStalenessMinutes;
        this.active = active;
        validateState();
    }

    public static DashboardMetricDefinition create(
            Agency agency,
            AnalyticsDashboardMetricType metricType,
            AnalyticsMetricScope metricScope,
            String metricName,
            String description,
            String sourceDomainKey,
            AnalyticsRefreshMode refreshMode,
            int maxStalenessMinutes) {
        return DashboardMetricDefinition.builder()
                .id(UUID.randomUUID())
                .agency(agency)
                .metricType(metricType)
                .metricScope(metricScope)
                .metricName(metricName)
                .description(description)
                .sourceDomainKey(sourceDomainKey)
                .refreshMode(refreshMode)
                .maxStalenessMinutes(maxStalenessMinutes)
                .active(true)
                .build();
    }

    public void refreshDefinition(
            AnalyticsMetricScope metricScope,
            String metricName,
            String description,
            String sourceDomainKey,
            AnalyticsRefreshMode refreshMode,
            int maxStalenessMinutes,
            boolean active) {
        this.metricScope = Objects.requireNonNull(metricScope, "metricScope must not be null");
        this.metricName = metricName;
        this.description = description;
        this.sourceDomainKey = sourceDomainKey;
        this.refreshMode = Objects.requireNonNull(refreshMode, "refreshMode must not be null");
        this.maxStalenessMinutes = maxStalenessMinutes;
        this.active = active;
        validateState();
    }

    @PrePersist
    @PreUpdate
    void normalize() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        metricType = Objects.requireNonNull(metricType, "metricType must not be null");
        metricScope = Objects.requireNonNull(metricScope, "metricScope must not be null");
        refreshMode = Objects.requireNonNull(refreshMode, "refreshMode must not be null");
        metricName = normalizeRequired(metricName, "metricName");
        description = normalizeRequired(description, "description");
        sourceDomainKey = normalizeRequired(sourceDomainKey, "sourceDomainKey");
        validateState();
    }

    private void validateState() {
        if (maxStalenessMinutes < 0) {
            throw new IllegalArgumentException("maxStalenessMinutes must be greater than or equal to 0");
        }
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
