package com.homehealthcare.messaging.application;

public class MessagingConflictException extends RuntimeException {

    public MessagingConflictException(String message) {
        super(message);
    }
}
