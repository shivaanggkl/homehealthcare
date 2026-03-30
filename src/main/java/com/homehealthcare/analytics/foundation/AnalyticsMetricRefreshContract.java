package com.homehealthcare.analytics.foundation;

public record AnalyticsMetricRefreshContract(
        AnalyticsDashboardMetricType metricType,
        AnalyticsRefreshMode refreshMode,
        int maxStalenessMinutes,
        String refreshExpectation) {
}
