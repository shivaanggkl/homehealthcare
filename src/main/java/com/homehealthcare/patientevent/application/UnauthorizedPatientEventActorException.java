package com.homehealthcare.patientevent.application;

public class UnauthorizedPatientEventActorException extends RuntimeException {

    public UnauthorizedPatientEventActorException(String message) {
        super(message);
    }
}
