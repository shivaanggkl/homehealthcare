package com.homehealthcare.messaging.application;

import java.util.UUID;

public class MessagingEntityNotFoundException extends RuntimeException {

    public MessagingEntityNotFoundException(String entityName, UUID entityId) {
        super(entityName + " not found: " + entityId);
    }
}
