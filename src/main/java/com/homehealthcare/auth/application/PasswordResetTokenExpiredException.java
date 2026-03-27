package com.homehealthcare.auth.application;

public class PasswordResetTokenExpiredException extends RuntimeException {

    public PasswordResetTokenExpiredException() {
        super("Password reset token has expired");
    }
}
