package com.homehealthcare.configuration.foundation;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedConfigurationActorException extends RuntimeException {

    public UnauthorizedConfigurationActorException(UUID actorMembershipId) {
        super("Agency membership " + actorMembershipId + " is not authorized for configuration changes");
    }
}
