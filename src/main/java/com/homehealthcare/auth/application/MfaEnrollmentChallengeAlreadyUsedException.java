package com.homehealthcare.auth.application;

public class MfaEnrollmentChallengeAlreadyUsedException extends RuntimeException {

    public MfaEnrollmentChallengeAlreadyUsedException() {
        super("MFA enrollment token is no longer available");
    }
}
