package com.homehealthcare.evv.application;

import java.util.UUID;

public class EvvEntityNotFoundException extends RuntimeException {

    public EvvEntityNotFoundException(String entityName, UUID entityId) {
        super(entityName + " was not found: " + entityId);
    }
}
