package com.homehealthcare.patient.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PatientConflictException extends RuntimeException {

    public PatientConflictException(String message) {
        super(message);
    }
}
