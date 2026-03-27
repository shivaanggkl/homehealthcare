package com.homehealthcare.auth.application;

public class MfaEnrollmentChallengeExpiredException extends RuntimeException {

    public MfaEnrollmentChallengeExpiredException() {
        super("MFA enrollment token has expired");
    }
}
