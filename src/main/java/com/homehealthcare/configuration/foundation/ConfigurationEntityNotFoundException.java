package com.homehealthcare.configuration.foundation;

import java.util.UUID;

public class ConfigurationEntityNotFoundException extends RuntimeException {

    public ConfigurationEntityNotFoundException(String entityType, UUID entityId) {
        super(entityType + " not found: " + entityId);
    }
}
