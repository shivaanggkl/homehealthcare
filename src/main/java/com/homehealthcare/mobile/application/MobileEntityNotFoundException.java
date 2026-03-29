package com.homehealthcare.mobile.application;

import java.util.UUID;

public class MobileEntityNotFoundException extends RuntimeException {

    public MobileEntityNotFoundException(String entityName, UUID entityId) {
        super(entityName + " not found: " + entityId);
    }
}
