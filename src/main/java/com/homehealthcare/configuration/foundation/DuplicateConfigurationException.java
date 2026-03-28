package com.homehealthcare.configuration.foundation;

import java.util.UUID;

public class DuplicateConfigurationException extends RuntimeException {

    public DuplicateConfigurationException(String entityType, UUID agencyId, String fieldName, String value) {
        super(entityType + " already exists in agency " + agencyId + " for " + fieldName + ": " + value);
    }
}
