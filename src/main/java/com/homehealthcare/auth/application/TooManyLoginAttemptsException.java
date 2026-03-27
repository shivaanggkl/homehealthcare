package com.homehealthcare.auth.application;

public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException() {
        super("Too many login attempts. Try again later.");
    }
}
