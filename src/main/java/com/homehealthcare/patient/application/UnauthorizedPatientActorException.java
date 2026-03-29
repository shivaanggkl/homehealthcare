package com.homehealthcare.patient.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedPatientActorException extends RuntimeException {

    public UnauthorizedPatientActorException(UUID membershipId) {
        super("Membership is not allowed to manage patient records: " + membershipId);
    }
}
