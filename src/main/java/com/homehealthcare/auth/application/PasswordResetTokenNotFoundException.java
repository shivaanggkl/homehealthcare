package com.homehealthcare.auth.application;

public class PasswordResetTokenNotFoundException extends RuntimeException {

    public PasswordResetTokenNotFoundException() {
        super("Password reset token is invalid");
    }
}
