package com.homehealthcare.auth.application;

public class CurrentAuthSessionNotFoundException extends RuntimeException {

    public CurrentAuthSessionNotFoundException() {
        super("Authenticated session is required");
    }
}
