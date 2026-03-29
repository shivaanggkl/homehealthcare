package com.homehealthcare.patient.application;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LikelyDuplicatePatientException extends RuntimeException {

    public LikelyDuplicatePatientException(UUID agencyId, String firstName, String lastName, LocalDate dateOfBirth) {
        super("Likely duplicate patient in agency " + agencyId
                + " for firstName=" + firstName
                + ", lastName=" + lastName
                + ", dateOfBirth=" + dateOfBirth);
    }
}
