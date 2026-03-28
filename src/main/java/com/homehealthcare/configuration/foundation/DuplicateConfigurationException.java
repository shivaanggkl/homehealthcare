package com.homehealthcare.configuration.foundation;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateConfigurationException extends RuntimeException {

    public DuplicateConfigurationException(String entityType, UUID agencyId, String fieldName, String value) {
        super(entityType + " already exists in agency " + agencyId + " for " + fieldName + ": " + value);
    }
}
