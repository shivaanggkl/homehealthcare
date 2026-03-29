package com.homehealthcare.scheduling.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedSchedulingActorException extends RuntimeException {

    public UnauthorizedSchedulingActorException(UUID membershipId) {
        super("Membership is not allowed to manage scheduling records: " + membershipId);
    }
}
