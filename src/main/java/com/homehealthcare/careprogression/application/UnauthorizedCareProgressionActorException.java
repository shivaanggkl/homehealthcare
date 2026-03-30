package com.homehealthcare.careprogression.application;

import java.util.UUID;

public class UnauthorizedCareProgressionActorException extends RuntimeException {

    public UnauthorizedCareProgressionActorException(UUID membershipId, String action) {
        super("Agency membership %s is not authorized to %s in the care progression workspace.".formatted(
                membershipId,
                action));
    }
}
