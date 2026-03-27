package com.homehealthcare.auth.application;

public class PasswordReuseNotAllowedException extends RuntimeException {

    public PasswordReuseNotAllowedException(int recentPasswordCount) {
        super("Password cannot match any of your last " + recentPasswordCount + " passwords");
    }
}
