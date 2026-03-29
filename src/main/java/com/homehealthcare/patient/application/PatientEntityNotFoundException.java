package com.homehealthcare.patient.application;

import java.util.UUID;

public class PatientEntityNotFoundException extends RuntimeException {

    public PatientEntityNotFoundException(String entityType, UUID entityId) {
        super(entityType + " was not found: " + entityId);
    }
}
