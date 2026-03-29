package com.homehealthcare.mobile.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedMobileActorException extends RuntimeException {

    public UnauthorizedMobileActorException(UUID actorMembershipId) {
        super("Actor membership is not allowed to execute this mobile action: " + actorMembershipId);
    }
}
