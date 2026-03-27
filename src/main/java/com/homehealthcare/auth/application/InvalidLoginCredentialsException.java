package com.homehealthcare.auth.application;

public class InvalidLoginCredentialsException extends RuntimeException {

    public InvalidLoginCredentialsException() {
        super("Invalid email or password");
    }
}
