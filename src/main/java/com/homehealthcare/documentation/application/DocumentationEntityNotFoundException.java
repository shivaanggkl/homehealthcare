package com.homehealthcare.documentation.application;

import java.util.UUID;

public class DocumentationEntityNotFoundException extends RuntimeException {

    public DocumentationEntityNotFoundException(String entityType, UUID entityId) {
        super(entityType + " not found: " + entityId);
    }
}
