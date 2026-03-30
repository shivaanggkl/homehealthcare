package com.homehealthcare.analytics.application;

import java.util.UUID;

public class AnalyticsEntityNotFoundException extends RuntimeException {

    public AnalyticsEntityNotFoundException(String entityName, UUID entityId) {
        super("%s %s was not found in the analytics workspace.".formatted(entityName, entityId));
    }
}
