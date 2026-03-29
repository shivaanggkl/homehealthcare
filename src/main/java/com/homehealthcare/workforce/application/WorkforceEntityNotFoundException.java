package com.homehealthcare.workforce.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkforceEntityNotFoundException extends RuntimeException {

    public WorkforceEntityNotFoundException(String entityType, UUID entityId) {
        super(entityType + " not found: " + entityId);
    }
}
