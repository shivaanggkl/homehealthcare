package com.homehealthcare.auth.application;

public class PasswordResetTokenAlreadyUsedException extends RuntimeException {

    public PasswordResetTokenAlreadyUsedException() {
        super("Password reset token is no longer available");
    }
}
