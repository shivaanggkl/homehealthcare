package com.homehealthcare.compliance.application;

import java.util.UUID;

public class ComplianceEntityNotFoundException extends RuntimeException {

    public ComplianceEntityNotFoundException(String entityName, UUID entityId) {
        super("%s %s was not found in the compliance workspace.".formatted(entityName, entityId));
    }

    public ComplianceEntityNotFoundException(String message) {
        super(message);
    }
}
