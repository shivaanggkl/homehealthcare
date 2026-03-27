package com.homehealthcare.auth.application;

public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(String message) {
        super(message);
    }
}
