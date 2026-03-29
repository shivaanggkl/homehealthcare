package com.homehealthcare.scheduling.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SchedulingEntityNotFoundException extends RuntimeException {

    public SchedulingEntityNotFoundException(String entityType, UUID entityId) {
        super(entityType + " not found: " + entityId);
    }
}
