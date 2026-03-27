package com.homehealthcare.auth.application;

public class InvalidTotpCodeException extends RuntimeException {

    public InvalidTotpCodeException() {
        super("TOTP code is invalid");
    }
}
