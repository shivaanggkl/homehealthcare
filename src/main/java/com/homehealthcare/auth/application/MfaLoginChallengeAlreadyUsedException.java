package com.homehealthcare.auth.application;

public class MfaLoginChallengeAlreadyUsedException extends RuntimeException {

    public MfaLoginChallengeAlreadyUsedException() {
        super("MFA login challenge is no longer available");
    }
}
