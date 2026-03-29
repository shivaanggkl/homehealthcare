package com.homehealthcare.workforce.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedWorkforceActorException extends RuntimeException {

    public UnauthorizedWorkforceActorException(UUID membershipId) {
        super("Membership is not allowed to manage workforce records: " + membershipId);
    }
}
