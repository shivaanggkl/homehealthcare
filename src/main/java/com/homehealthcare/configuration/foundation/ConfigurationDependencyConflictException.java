package com.homehealthcare.configuration.foundation;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConfigurationDependencyConflictException extends RuntimeException {

    public ConfigurationDependencyConflictException(String entityType, UUID entityId, String dependentUsageDescription) {
        super(entityType + " cannot be deactivated because it is still referenced by " + dependentUsageDescription + ": " + entityId);
    }
}
