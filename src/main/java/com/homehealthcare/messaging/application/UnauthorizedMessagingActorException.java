package com.homehealthcare.messaging.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedMessagingActorException extends RuntimeException {

    public UnauthorizedMessagingActorException(UUID actorMembershipId) {
        super("Actor membership is not allowed to perform this messaging action: " + actorMembershipId);
    }
}
