package com.homehealthcare.analytics.application;

import java.util.UUID;

public class UnauthorizedAnalyticsActorException extends RuntimeException {

    public UnauthorizedAnalyticsActorException(UUID membershipId, String action) {
        super("Agency membership %s is not authorized to %s in the analytics workspace.".formatted(membershipId, action));
    }
}
