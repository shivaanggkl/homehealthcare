package com.homehealthcare.auth.application;

public class MfaLoginChallengeNotFoundException extends RuntimeException {

    public MfaLoginChallengeNotFoundException() {
        super("MFA login challenge is invalid");
    }
}
