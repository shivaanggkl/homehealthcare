package com.homehealthcare.patientevent.application;

public class PatientEventConflictException extends RuntimeException {

    public PatientEventConflictException(String message) {
        super(message);
    }
}
