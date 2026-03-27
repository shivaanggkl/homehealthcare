package com.homehealthcare.auth.application;

public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException() {
        super("Password must be at least 12 characters and include upper, lower, digit, and symbol characters");
    }
}
