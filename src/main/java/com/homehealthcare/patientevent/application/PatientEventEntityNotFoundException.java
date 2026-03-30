package com.homehealthcare.patientevent.application;

import java.util.UUID;

public class PatientEventEntityNotFoundException extends RuntimeException {

    public PatientEventEntityNotFoundException(String entityName, UUID entityId) {
        super("%s %s was not found in the patient-event workspace.".formatted(entityName, entityId));
    }
}
