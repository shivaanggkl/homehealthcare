package com.homehealthcare.auth.application;

public class CurrentPasswordMismatchException extends RuntimeException {

    public CurrentPasswordMismatchException() {
        super("Current password is incorrect");
    }
}
