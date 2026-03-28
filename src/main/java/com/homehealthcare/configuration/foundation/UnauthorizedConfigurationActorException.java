package com.homehealthcare.configuration.foundation;

import java.util.UUID;

public class UnauthorizedConfigurationActorException extends RuntimeException {

    public UnauthorizedConfigurationActorException(UUID actorMembershipId) {
        super("Agency membership " + actorMembershipId + " is not authorized for configuration changes");
    }
}
