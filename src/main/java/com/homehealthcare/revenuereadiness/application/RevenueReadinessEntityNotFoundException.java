package com.homehealthcare.revenuereadiness.application;

import java.util.UUID;

public class RevenueReadinessEntityNotFoundException extends RuntimeException {

    public RevenueReadinessEntityNotFoundException(String entityName, UUID entityId) {
        super("%s %s was not found in the revenue-readiness workspace.".formatted(entityName, entityId));
    }
}
