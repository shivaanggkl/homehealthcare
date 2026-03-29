package com.homehealthcare.patient.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPatientStateTransitionException extends RuntimeException {

    public InvalidPatientStateTransitionException(String message) {
        super(message);
    }
}
