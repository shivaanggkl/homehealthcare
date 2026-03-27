package com.homehealthcare.auth.application;

public class MfaEnrollmentRequiredException extends RuntimeException {

    public MfaEnrollmentRequiredException() {
        super("MFA enrollment is required before login can complete");
    }
}
