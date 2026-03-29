package com.homehealthcare.patient.application;

import java.util.UUID;

public class UnauthorizedPatientActorException extends RuntimeException {

    public UnauthorizedPatientActorException(UUID membershipId) {
        super("Membership is not allowed to manage patient records: " + membershipId);
    }
}
