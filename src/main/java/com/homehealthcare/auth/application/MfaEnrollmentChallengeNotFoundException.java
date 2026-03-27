package com.homehealthcare.auth.application;

public class MfaEnrollmentChallengeNotFoundException extends RuntimeException {

    public MfaEnrollmentChallengeNotFoundException() {
        super("MFA enrollment token is invalid");
    }
}
