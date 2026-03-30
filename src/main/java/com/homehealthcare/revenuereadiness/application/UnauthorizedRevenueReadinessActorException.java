package com.homehealthcare.revenuereadiness.application;

import java.util.UUID;

public class UnauthorizedRevenueReadinessActorException extends RuntimeException {

    public UnauthorizedRevenueReadinessActorException(UUID membershipId, String action) {
        super("Agency membership %s is not authorized to %s in the revenue-readiness workspace.".formatted(membershipId, action));
    }
}
