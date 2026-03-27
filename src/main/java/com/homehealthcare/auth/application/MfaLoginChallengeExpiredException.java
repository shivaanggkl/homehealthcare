package com.homehealthcare.auth.application;

public class MfaLoginChallengeExpiredException extends RuntimeException {

    public MfaLoginChallengeExpiredException() {
        super("MFA login challenge has expired");
    }
}
